# VividBatis

![Build](https://github.com/only52607/VividBatis/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

![logo](src/main/resources/META-INF/pluginIcon.svg)

<!-- Plugin description -->
Tired of debugging complex dynamic SQL in MyBatis? Wasting time on typos in your OGNL expressions? VividBatis is here to relieve your pain!

It brings you two core features:

1.  **One-Click SQL Preview**: Instantly preview the rendered SQL statement with a single click on the icon next to the SQL tag in your mapper XML files. Say goodbye to the tedious process of starting your project and debugging. Make your SQL debugging intuitive and efficient.

2.  **OGNL Syntax Highlighting**: The plugin provides clear and eye-catching syntax highlighting for OGNL expressions. Whether it's `if`, `choose`, `when`, or `foreach`, everything is clear at a glance, helping you write complex dynamic SQL with ease and avoid silly mistakes.
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