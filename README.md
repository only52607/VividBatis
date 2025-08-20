# VividBatis

![Build](https://github.com/only52607/VividBatis/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
你是否曾为调试 MyBatis 动态 SQL 而烦恼？是否曾因 OGNL 表达式的拼写错误而浪费大量时间？VividBatis 插件正是为了解决这些痛点而生！

它为你带来了两大核心功能：

1.  **SQL 预览，快人一步**：在 Mapper.xml 文件中，只需轻轻一点，即可实时预览渲染后的 SQL 语句。告别繁琐的项目启动和调试过程，让 SQL 调试变得直观、高效。

    ![preview.gif](./gif/preview.gif)

2.  **OGNL 语法高亮，告别手误**：插件为 OGNL 表达式提供了清晰、醒目的语法高亮。无论是 `if`, `choose`, `when`, 还是 `foreach`，都能一目了然，让你在编写复杂动态 SQL 时也能游刃有余，有效避免低级错误。
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "VividBatis"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/only52607/VividBatis/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

This project is licensed under the [GNU General Public License, Version 3.0](LICENSE).

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
