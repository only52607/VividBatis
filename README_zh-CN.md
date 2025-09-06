# ![logo](src/main/resources/META-INF/pluginIcon.svg) VividBatis

![Build](https://github.com/only52607/VividBatis/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/28245-vividbatis.svg)](https://plugins.jetbrains.com/plugin/28245-vividbatis)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28245-vividbatis.svg)](https://plugins.jetbrains.com/plugin/28245-vividbatis)

<!-- Plugin description -->
VividBatis: SQL预览与OGNL高亮。
此插件协助处理MyBatis动态SQL和OGNL表达式。

主要功能：

*   **一键SQL预览**：直接从mapper XML文件即时预览渲染后的SQL语句。这消除了项目启动和手动调试的需要。
*   **OGNL语法高亮**：为OGNL表达式提供语法高亮，包括`if`、`choose`、`when`和`foreach`。这提高了可读性并有助于防止常见错误。
<!-- Plugin description end -->

![logo](gif/preview.gif)

## 安装

- 使用IDE内置插件系统：
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>搜索 "VividBatis"</kbd> >
  <kbd>安装</kbd>
  
- 使用JetBrains Marketplace：

  前往[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)并通过点击<kbd>安装到...</kbd>按钮进行安装，如果您的IDE正在运行。

  您也可以从[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions)下载[最新版本](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions)并手动安装，方法是：
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>从磁盘安装插件...</kbd>

- 手动安装：

  下载[最新版本](https://github.com/only52607/VividBatis/releases/latest)并手动安装，方法是：
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>从磁盘安装插件...</kbd>

## 许可证

本项目采用[GNU通用公共许可证，版本3.0](LICENSE)授权。

---
插件基于[IntelliJ Platform Plugin Template][template]。

[template]: https://github.com/JetBrains/intellij-platform-plugin-template