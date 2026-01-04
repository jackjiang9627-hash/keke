#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
大语言模型插件

提供AI对话能力：
- chat: 与大模型对话
- list_models: 列出可用模型
- get_model_info: 获取当前模型信息

支持的模型后端：
1. 阿里云千问 (qwen) - 通过dashscope SDK调用
2. 本地Ollama模型 - 通过HTTP API调用
3. Mock模式 - 用于测试或无模型时的降级方案
"""

import logging
import os
import json
from typing import Any, Dict, List, Optional
from abc import ABC, abstractmethod

# 添加父目录到路径以导入PluginBase
import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from plugin_base import PluginBase


class LlmBackend(ABC):
    """LLM后端基类"""
    
    @abstractmethod
    def get_name(self) -> str:
        """返回后端名称"""
        pass
    
    @abstractmethod
    def is_available(self) -> bool:
        """检查后端是否可用"""
        pass
    
    @abstractmethod
    def chat(self, messages: List[Dict], model: str = None, **kwargs) -> str:
        """
        对话接口
        
        Args:
            messages: 对话历史 [{"role": "user/assistant", "content": "..."}]
            model: 模型名称（可选）
            **kwargs: 其他参数
            
        Returns:
            模型回复内容
        """
        pass
    
    @abstractmethod
    def list_models(self) -> List[str]:
        """列出可用模型"""
        pass


class QwenBackend(LlmBackend):
    """阿里云千问后端"""
    
    def __init__(self, api_key: str = None):
        self.logger = logging.getLogger('QwenBackend')
        self.api_key = api_key or os.environ.get('DASHSCOPE_API_KEY')
        self._sdk_available = False
        self._check_availability()
    
    def _check_availability(self):
        """检查dashscope SDK是否可用"""
        try:
            import dashscope
            self._sdk_available = True
            if self.api_key:
                dashscope.api_key = self.api_key
                self.logger.info("千问后端已初始化（使用环境变量 API Key）")
            else:
                self.logger.info("千问后端已初始化（API Key 将从数据库读取）")
        except ImportError:
            self.logger.warning("dashscope SDK未安装，千问后端不可用")
    
    def get_name(self) -> str:
        return 'qwen'
    
    def is_available(self) -> bool:
        # 只要 SDK 安装了就标记为可用，API Key 会在调用时从数据库传入
        return self._sdk_available
    
    def chat(self, messages: List[Dict], model: str = None, api_key: str = None, **kwargs) -> str:
        if not self._sdk_available:
            raise RuntimeError("千问后端不可用：dashscope SDK 未安装")
        
        # 确保有 API Key
        effective_api_key = api_key or self.api_key
        if not effective_api_key:
            raise RuntimeError("千问后端不可用：未配置 API Key，请在系统设置中配置")
        
        import dashscope
        from dashscope import Generation
        
        # 设置 API Key
        dashscope.api_key = effective_api_key
        
        # 默认使用qwen-turbo
        model = model or 'qwen-turbo'
        
        response = Generation.call(
            model=model,
            messages=messages,
            result_format='message'
        )
        
        if response.status_code == 200:
            return response.output.choices[0].message.content
        else:
            raise RuntimeError(f"千问API调用失败: {response.code} - {response.message}")
    
    def list_models(self) -> List[str]:
        return [
            'qwen-turbo',
            'qwen-plus', 
            'qwen-max',
            'qwen-max-longcontext'
        ]
    
    def chat_stream(self, messages: List[Dict], model: str = None, api_key: str = None, **kwargs) -> List[Dict]:
        """流式对话，返回分块列表"""
        if not self._sdk_available:
            raise RuntimeError("千问后端不可用：dashscope SDK 未安装")
        
        effective_api_key = api_key or self.api_key
        if not effective_api_key:
            raise RuntimeError("千问后端不可用：未配置 API Key")
        
        import dashscope
        from dashscope import Generation
        
        dashscope.api_key = effective_api_key
        model = model or 'qwen-turbo'
        
        chunks = []
        
        # 使用流式调用
        responses = Generation.call(
            model=model,
            messages=messages,
            result_format='message',
            stream=True,
            incremental_output=True  # 增量输出
        )
        
        for response in responses:
            if response.status_code == 200:
                if response.output and response.output.choices:
                    content = response.output.choices[0].message.content
                    if content:
                        chunks.append({'content': content})
            else:
                self.logger.error(f"流式调用错误: {response.code} - {response.message}")
        
        return chunks


class OllamaBackend(LlmBackend):
    """本地Ollama后端"""
    
    def __init__(self, base_url: str = None):
        self.logger = logging.getLogger('OllamaBackend')
        self.base_url = base_url or os.environ.get('OLLAMA_BASE_URL', 'http://localhost:11434')
        self._available = False
        self._models = []
        self._check_availability()
    
    def _check_availability(self):
        """检查Ollama服务是否可用"""
        try:
            import requests
            response = requests.get(f"{self.base_url}/api/tags", timeout=3)
            if response.status_code == 200:
                self._available = True
                data = response.json()
                self._models = [m['name'] for m in data.get('models', [])]
                self.logger.info(f"Ollama后端已初始化，可用模型: {self._models}")
            else:
                self.logger.warning("Ollama服务响应异常")
        except Exception as e:
            self.logger.warning(f"Ollama服务不可用: {e}")
    
    def get_name(self) -> str:
        return 'ollama'
    
    def is_available(self) -> bool:
        return self._available
    
    def chat(self, messages: List[Dict], model: str = None, **kwargs) -> str:
        if not self._available:
            raise RuntimeError("Ollama后端不可用")
        
        import requests
        
        # 默认使用第一个可用模型或qwen2.5
        model = model or (self._models[0] if self._models else 'qwen2.5:1.5b')
        
        response = requests.post(
            f"{self.base_url}/api/chat",
            json={
                "model": model,
                "messages": messages,
                "stream": False
            },
            timeout=60
        )
        
        if response.status_code == 200:
            data = response.json()
            return data.get('message', {}).get('content', '')
        else:
            raise RuntimeError(f"Ollama调用失败: {response.status_code}")
    
    def list_models(self) -> List[str]:
        return self._models


class MockBackend(LlmBackend):
    """Mock后端（降级方案）"""
    
    def __init__(self):
        self.logger = logging.getLogger('MockBackend')
    
    def get_name(self) -> str:
        return 'mock'
    
    def is_available(self) -> bool:
        return True
    
    def chat(self, messages: List[Dict], model: str = None, **kwargs) -> str:
        # 获取最后一条用户消息
        last_message = ""
        for msg in reversed(messages):
            if msg.get('role') == 'user':
                last_message = msg.get('content', '')
                break
        
        # 简单的关键词匹配回复
        lower_msg = last_message.lower()
        
        if '待办' in lower_msg or 'todo' in lower_msg:
            return "您可以在左侧导航栏点击**待办事项**模块来管理您的待办。\n\n需要我帮您添加新的待办吗？"
        
        if '案例' in lower_msg or 'case' in lower_msg:
            return "您可以在**案例库**中搜索和管理案例。\n\n支持的操作：\n- 新增案例\n- 搜索案例\n- 导入/导出Excel"
        
        if '日志' in lower_msg or 'log' in lower_msg:
            return "**日志分析**模块支持：\n- 上传日志文件解析\n- 日志级别统计\n- 错误日志筛选\n\n请上传日志文件开始分析。"
        
        if '监控' in lower_msg or 'monitor' in lower_msg:
            return "**系统监控**功能包括：\n- CPU/内存/磁盘使用率\n- 网络IO监控\n- 进程监控\n\n请到监控页面查看详情。"
        
        if 'ssh' in lower_msg or '服务器' in lower_msg or '设备' in lower_msg:
            return "**SSH批量运维**支持：\n- 设备管理\n- 批量命令执行\n- 文件传输\n\n请先添加设备后使用。"
        
        if '你好' in lower_msg or 'hello' in lower_msg or 'hi' in lower_msg:
            return "您好！我是智能助手，可以帮您：\n\n1. 管理待办事项\n2. 搜索案例库\n3. 分析日志文件\n4. 查看系统监控\n5. SSH批量运维\n\n请问有什么可以帮您的？"
        
        return "感谢您的提问！作为智能助手，我可以帮您处理以下任务：\n\n- 待办事项管理\n- 案例库搜索\n- 日志分析\n- 系统监控\n- SSH运维\n\n请告诉我您需要什么帮助？"
    
    def list_models(self) -> List[str]:
        return ['mock']


class LlmPlugin(PluginBase):
    """大语言模型插件"""
    
    def __init__(self):
        self.logger = logging.getLogger('LlmPlugin')
        self.backends: Dict[str, LlmBackend] = {}
        self.default_backend: str = 'mock'
    
    def get_name(self) -> str:
        return 'llm'
    
    def get_methods(self) -> list:
        return [
            'chat',
            'chat_stream',
            'list_models',
            'get_backends',
            'set_default_backend'
        ]
    
    def initialize(self):
        """初始化：加载所有可用的后端"""
        self.logger.info("初始化LLM插件...")
        
        # 初始化所有后端
        self.backends['mock'] = MockBackend()
        self.backends['qwen'] = QwenBackend()
        self.backends['ollama'] = OllamaBackend()
        
        # 选择默认后端（优先使用千问，其次Ollama，最后Mock）
        if self.backends['qwen'].is_available():
            self.default_backend = 'qwen'
            self.logger.info("默认后端: 千问")
        elif self.backends['ollama'].is_available():
            self.default_backend = 'ollama'
            self.logger.info("默认后端: Ollama")
        else:
            self.default_backend = 'mock'
            self.logger.info("默认后端: Mock (请配置API Key或启动Ollama)")
        
        self.logger.info("LLM插件初始化完成")
    
    def execute(self, method: str, params: dict) -> Any:
        """执行方法"""
        if method == 'chat':
            return self._chat(params)
        elif method == 'chat_stream':
            return self._chat_stream(params)
        elif method == 'list_models':
            return self._list_models(params)
        elif method == 'get_backends':
            return self._get_backends()
        elif method == 'set_default_backend':
            return self._set_default_backend(params)
        else:
            raise ValueError(f"未知方法: {method}, 支持的方法: {self.get_methods()}")
    
    def _chat(self, params: dict) -> dict:
        """
        与大模型对话
        
        Args:
            params: {
                "messages": [{"role": "user", "content": "..."}],
                "backend": "qwen/ollama/mock" (可选，默认使用default_backend),
                "model": "模型名称" (可选),
                "api_key": "API密钥" (可选，从数据库传入)
            }
            
        Returns:
            {"reply": "模型回复", "backend": "使用的后端", "model": "使用的模型"}
        """
        messages = params.get('messages', [])
        backend_name = params.get('backend', self.default_backend)
        model = params.get('model')
        api_key = params.get('api_key')  # 从数据库传入的 API Key
        
        if not messages:
            raise ValueError("messages参数不能为空")
        
        # 如果有 API Key 传入，并且后端是 qwen，可能需要重新检查可用性
        if api_key and backend_name == 'qwen':
            backend = self.backends.get('qwen')
            # 即使之前不可用，有 API Key 也尝试调用
        else:
            # 获取后端
            backend = self.backends.get(backend_name)
            if backend is None or not backend.is_available():
                self.logger.warning(f"后端 {backend_name} 不可用，使用Mock")
                backend = self.backends['mock']
                backend_name = 'mock'
        
        # 调用后端
        try:
            reply = backend.chat(messages, model, api_key=api_key)
            return {
                'reply': reply,
                'backend': backend_name,
                'model': model or 'default'
            }
        except Exception as e:
            self.logger.error(f"调用后端失败: {e}")
            # 降级到Mock
            if backend_name != 'mock':
                self.logger.info("降级到Mock后端")
                reply = self.backends['mock'].chat(messages)
                return {
                    'reply': reply,
                    'backend': 'mock',
                    'model': 'mock'
                }
            raise
    
    def _chat_stream(self, params: dict) -> dict:
        """
        流式对话
        
        Args:
            params: {
                "messages": [{"role": "user", "content": "..."}],
                "backend": "qwen/ollama/mock" (可选),
                "model": "模型名称" (可选),
                "api_key": "API密钥" (可选)
            }
            
        Returns:
            {"chunks": [{"content": "..."}], "backend": "...", "model": "..."}
        """
        messages = params.get('messages', [])
        backend_name = params.get('backend', self.default_backend)
        model = params.get('model')
        api_key = params.get('api_key')
        
        if not messages:
            raise ValueError("messages参数不能为空")
        
        # 获取后端
        backend = self.backends.get(backend_name)
        
        # 检查后端是否支持流式输出
        if hasattr(backend, 'chat_stream') and backend.is_available():
            try:
                chunks = backend.chat_stream(messages, model, api_key=api_key)
                return {
                    'chunks': chunks,
                    'backend': backend_name,
                    'model': model or 'default'
                }
            except Exception as e:
                self.logger.error(f"流式调用失败: {e}")
        
        # 降级到普通 chat，然后分块返回
        self.logger.info(f"后端 {backend_name} 不支持流式，使用普通模式")
        result = self._chat(params)
        reply = result.get('reply', '')
        
        # 将完整回复分块
        chunks = [{'content': reply}] if reply else []
        return {
            'chunks': chunks,
            'backend': result.get('backend'),
            'model': result.get('model')
        }
    
    def _list_models(self, params: dict) -> dict:
        """
        列出可用模型
        
        Args:
            params: {"backend": "qwen/ollama/mock" (可选)}
            
        Returns:
            {"models": {"qwen": [...], "ollama": [...], ...}}
        """
        backend_name = params.get('backend')
        
        if backend_name:
            backend = self.backends.get(backend_name)
            if backend and backend.is_available():
                return {'models': {backend_name: backend.list_models()}}
            else:
                return {'models': {}}
        
        # 返回所有可用后端的模型
        result = {}
        for name, backend in self.backends.items():
            if backend.is_available():
                result[name] = backend.list_models()
        
        return {'models': result}
    
    def _get_backends(self) -> dict:
        """
        获取所有后端状态
        
        Returns:
            {"backends": [{"name": "qwen", "available": true, "default": true}, ...]}
        """
        backends = []
        for name, backend in self.backends.items():
            backends.append({
                'name': name,
                'available': backend.is_available(),
                'default': name == self.default_backend
            })
        return {'backends': backends}
    
    def _set_default_backend(self, params: dict) -> dict:
        """
        设置默认后端
        
        Args:
            params: {"backend": "qwen/ollama/mock"}
            
        Returns:
            {"success": true, "backend": "qwen"}
        """
        backend_name = params.get('backend')
        
        if backend_name not in self.backends:
            raise ValueError(f"未知后端: {backend_name}")
        
        if not self.backends[backend_name].is_available():
            raise ValueError(f"后端 {backend_name} 不可用")
        
        self.default_backend = backend_name
        self.logger.info(f"默认后端已切换为: {backend_name}")
        
        return {'success': True, 'backend': backend_name}
    
    def shutdown(self):
        """关闭插件"""
        self.logger.info("LLM插件已关闭")
