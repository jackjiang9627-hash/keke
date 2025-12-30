#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Python Gateway - 统一入口

提供通用的Python调用能力：
- 插件机制：动态加载和执行不同的模块
- 任务处理：从Java端获取任务并执行
- 结果回调：将结果返回给Java端

支持的模块：
- semantic: 语义分析（embedding、相似度计算）
- 可扩展其他模块
"""

import sys
import os
import json
import logging
import importlib
import traceback
from typing import Any, Dict, Optional
from abc import ABC, abstractmethod

# 添加当前目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('PythonGateway')


class PluginBase(ABC):
    """插件基类"""
    
    @abstractmethod
    def get_name(self) -> str:
        """返回插件名称"""
        pass
    
    @abstractmethod
    def get_methods(self) -> list:
        """返回支持的方法列表"""
        pass
    
    @abstractmethod
    def execute(self, method: str, params: dict) -> Any:
        """
        执行方法
        
        Args:
            method: 方法名
            params: 参数字典
            
        Returns:
            执行结果
        """
        pass
    
    def initialize(self):
        """初始化插件（可选覆盖）"""
        pass
    
    def shutdown(self):
        """关闭插件（可选覆盖）"""
        pass


class PluginRegistry:
    """插件注册表"""
    
    def __init__(self):
        self.plugins: Dict[str, PluginBase] = {}
        self.logger = logging.getLogger('PluginRegistry')
    
    def register(self, plugin: PluginBase):
        """注册插件"""
        name = plugin.get_name()
        self.plugins[name] = plugin
        self.logger.info(f"注册插件: {name}, 方法: {plugin.get_methods()}")
    
    def get(self, name: str) -> Optional[PluginBase]:
        """获取插件"""
        return self.plugins.get(name)
    
    def list_plugins(self) -> list:
        """列出所有插件"""
        return list(self.plugins.keys())
    
    def initialize_all(self):
        """初始化所有插件"""
        for name, plugin in self.plugins.items():
            try:
                plugin.initialize()
                self.logger.info(f"插件初始化成功: {name}")
            except Exception as e:
                self.logger.error(f"插件初始化失败: {name}, 错误: {e}")
    
    def shutdown_all(self):
        """关闭所有插件"""
        for name, plugin in self.plugins.items():
            try:
                plugin.shutdown()
                self.logger.info(f"插件关闭: {name}")
            except Exception as e:
                self.logger.error(f"插件关闭失败: {name}, 错误: {e}")


class PythonGateway:
    """Python网关 - 统一入口"""
    
    def __init__(self, gateway_port: int = 25333):
        self.gateway_port = gateway_port
        self.registry = PluginRegistry()
        self.running = False
        self.gateway = None
        self.entry_point = None
        self.logger = logging.getLogger('PythonGateway')
    
    def load_plugins(self):
        """加载插件"""
        # 导入并注册所有插件
        plugins_dir = os.path.join(os.path.dirname(__file__), 'plugins')
        
        if not os.path.exists(plugins_dir):
            os.makedirs(plugins_dir)
            self.logger.info(f"创建插件目录: {plugins_dir}")
        
        # 动态加载plugins目录下的所有插件
        for filename in os.listdir(plugins_dir):
            if filename.endswith('_plugin.py'):
                module_name = filename[:-3]  # 去掉.py
                try:
                    # 动态导入模块
                    spec = importlib.util.spec_from_file_location(
                        module_name, 
                        os.path.join(plugins_dir, filename)
                    )
                    module = importlib.util.module_from_spec(spec)
                    spec.loader.exec_module(module)
                    
                    # 查找并注册插件类
                    for attr_name in dir(module):
                        attr = getattr(module, attr_name)
                        if (isinstance(attr, type) and 
                            issubclass(attr, PluginBase) and 
                            attr is not PluginBase):
                            plugin = attr()
                            self.registry.register(plugin)
                            
                except Exception as e:
                    self.logger.error(f"加载插件失败: {filename}, 错误: {e}")
                    traceback.print_exc()
    
    def execute_task(self, task_json: str) -> str:
        """
        执行任务
        
        Args:
            task_json: 任务JSON字符串，格式：
                {
                    "taskId": "xxx",
                    "module": "semantic",
                    "method": "compute_embedding",
                    "params": {...}
                }
        
        Returns:
            结果JSON字符串
        """
        try:
            task = json.loads(task_json)
            task_id = task.get('taskId', 'unknown')
            module = task.get('module', '')
            method = task.get('method', '')
            params = task.get('params', {})
            
            self.logger.info(f"执行任务: taskId={task_id}, module={module}, method={method}")
            
            # 获取插件
            plugin = self.registry.get(module)
            if plugin is None:
                raise ValueError(f"未知模块: {module}, 可用模块: {self.registry.list_plugins()}")
            
            # 执行方法
            result = plugin.execute(method, params)
            
            self.logger.info(f"任务完成: taskId={task_id}")
            
            return json.dumps({
                'success': True,
                'result': result
            })
            
        except Exception as e:
            self.logger.error(f"任务执行失败: {e}")
            traceback.print_exc()
            return json.dumps({
                'success': False,
                'error': str(e)
            })
    
    def connect_java(self):
        """连接到Java端的Py4J网关"""
        try:
            from py4j.java_gateway import JavaGateway, GatewayParameters
            
            self.gateway = JavaGateway(
                gateway_parameters=GatewayParameters(port=self.gateway_port)
            )
            self.entry_point = self.gateway.entry_point
            self.logger.info(f"已连接到Java网关: port={self.gateway_port}")
            return True
            
        except Exception as e:
            self.logger.error(f"连接Java网关失败: {e}")
            return False
    
    def run(self):
        """主循环：从Java获取任务并执行"""
        self.running = True
        self.logger.info("Python Gateway 启动")
        
        # 加载插件
        self.load_plugins()
        
        # 初始化所有插件
        self.registry.initialize_all()
        
        # 连接Java
        if not self.connect_java():
            self.logger.error("无法连接到Java端，退出")
            return
        
        try:
            while self.running:
                try:
                    # 从Java获取任务（阻塞5秒）
                    task_json = self.entry_point.pollTask(5000)
                    
                    if task_json is None:
                        continue
                    
                    # 解析任务
                    task = json.loads(task_json)
                    task_id = task.get('taskId')
                    
                    # 执行任务
                    result_json = self.execute_task(task_json)
                    result = json.loads(result_json)
                    
                    # 返回结果给Java
                    if result.get('success'):
                        self.entry_point.completeTask(
                            task_id, 
                            json.dumps(result.get('result'))
                        )
                    else:
                        self.entry_point.failTask(
                            task_id, 
                            result.get('error', 'Unknown error')
                        )
                        
                except Exception as e:
                    self.logger.error(f"任务处理异常: {e}")
                    traceback.print_exc()
                    
        except KeyboardInterrupt:
            self.logger.info("收到中断信号")
        finally:
            self.shutdown()
    
    def shutdown(self):
        """关闭网关"""
        self.running = False
        self.registry.shutdown_all()
        if self.gateway:
            self.gateway.close()
        self.logger.info("Python Gateway 关闭")


def main():
    """入口函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Python Gateway')
    parser.add_argument('--port', type=int, default=25333, help='Java网关端口')
    args = parser.parse_args()
    
    gateway = PythonGateway(gateway_port=args.port)
    gateway.run()


if __name__ == '__main__':
    main()
