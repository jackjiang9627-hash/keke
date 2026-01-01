#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
插件基类模块

定义所有插件必须实现的接口
"""

from abc import ABC, abstractmethod
from typing import Any, Dict


class PluginBase(ABC):
    """插件基类，所有插件必须继承此类"""
    
    @abstractmethod
    def get_name(self) -> str:
        """返回插件名称，用于模块注册"""
        pass
    
    @abstractmethod
    def execute(self, method: str, params: Dict[str, Any]) -> Any:
        """执行插件方法"""
        pass
    
    def get_methods(self) -> list:
        """返回插件支持的方法列表"""
        return []
