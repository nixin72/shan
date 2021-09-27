# Shan

Shan is a declarative wrapper around your favourite package manager.

- Cross-platform: works on Linux, Windows and MacOS
- Simple: shan harnesses existing package managers to do the heavy lifting
- Declarative: sync to your config or let shan build it for you
- Wide support: shan supports tons of package managers out of the box
- Extensible: your package manager not supported? Add it in ~3 lines of code
- Fast: shan is compiled using GraalVM to make it super fast - no JVM here.

**NOTE:** Shan still isn't at 1.0 release, and as of now, it in fact does _not_ 
support tons of package managers. My goal is, as of 1.0, to support:
- dpkg
- yay
- paru
- dnf
- brew
- chocolatey
- winget
- npm
- pip
- Gems

## Table of Contents
- [Why use shan?](#why-shan)
- [Using shan](#usage)
  - [Installing packages](#installing-packages)
  - [Removing packages](#removing-packages)
  - [Your config](#your-config)
  - [Temporary installs](#temporary-installs)
- [Contributing](#contributing)
  - [Adding a package manager](#adding-a-package-manager)
- [License](#license)

## Why Shan?

Shan is for people who want to be able to manage their packages declaratively,
but don't want the complexity that comes with other systems like NixOS or Guix. 
It's aim is to facilitate setting up new systems. If you've bought a new laptop
and want to install everything you had with `brew` or you've decided you want to 
try out Linux and want to install you had in `chocolatey`. Maybe you like 
distro-hopping and want a way to get started quickly with all the packages you 
know and love, or maybe you just broke your Arch install and decided it's easier
to nuke your system than fix the issue (I know I've been there).

If you've ever been in any of these situations, try out shan. Shan works for 
everyone, no matter what operating system you're using, no matter what package 
managers you're using.

## Usage

### Installing packages
Installing packages with shan is easily done through the `install` or `in` 
subcommand. 
``` sh
shan install nodejs
```
For example will install `nodejs` using the [default package manager](#your-config).

You can also specify to shan which package manager should be used to install a 
package:
``` sh
shan install nodejs :npm react
```
And this will install `nodejs` using the default package manager, and then `react`
using npm.

You can also mix and match things in any order and install as many packages as you
want in a single command:

``` sh
shan install nodejs python :npm react react-native :pip PyYAML wakatime :npm expo expo-cli
```
Every time you install a package, the package will also be added to 
[your config file](#your-config) so that down the road, you'll have everything
available to you in a declarative config.

### Removing packages
When removing a package using Shan, it's generally able to figure out which 
package manager it should be using to remove a package. Let's say we have the 
following [config file](#your-config):
```clojure
{:yay [nodejs neovim atop]
 :npm [react underscore atop]}
```
Now, if we want to remove `nodejs`, we simply have to do:

``` sh
shan remove nodejs
```
And shan will figure out which package manager to remove nodejs using. Unlike 
installing, where it'll install using your default package manager, removing will
simply remove from where the package exists. 

``` sh
shan remove nodejs neovim react
```
Will remove `nodejs` and `neovim` using yay, and `react` will be removed using 
`npm`.

We do however have `atop` installed here in both yay *and* npm. In this case,
Shan will prompt you for which package manager you want to install `atop` from.

A package also doesn't need to be in your config to uninstall it. 
``` sh
shan remove emacs
```
If you remove a package that isn't in your config, shan will look at which 
package mangers are available on your system, then check if that package is
installed using any of those package managers. If it is, then it'll remove the 
package using that package manager, and let you know where it found it.

### Your config
Shan supports configs written in JSON, YAML, or Clojure's own data format, EDN.
The examples here will be written in EDN, however they translate intuitively
to JSON and YAML.

The config for Shan is very simple, and requires nothing to get started. In fact,
if you have no config, `shan install :yay vim` would generate the following config
for you:
``` clojure
{:yay [vim]}
```
Every time you install or remove a package, this change is reflected in your 
config file. Given the simple config above,
``` sh
shan install :npm react
```
Would modify your config file to the following:
``` clojure
{:yay [vim],
 :npm [react]}
```

#### Default package manager
One thing that's very useful to have specified in your config is a default 
package manager. Without this specified, you will need to specify a package 
manager for everything you install. 
``` clojure
{:default :yay
 :yay [vim]}
```
Having a `default` key in your config specifies which package manager shan should
use when none are specified.

You can set a default package manager using
``` sh
shan default <package-manager>
```

### Temporary packages
Sometimes you want to quickly install a package to test it out, but you don't 
necessarily want it to be added to your config. When installing or removing a 
package, shan supports the ability to make "temporary" changes. This means that
the changes you make won't get added to your config file.

``` sh
shan install -t nodejs :npm react
```
Will install nodejs using the default package manager and react using npm, however
neither of these packages will get added to your config. Instead, the changes will
be saved in a separate file. This way, shan can act against that file independently 
of your config. For example if you want to uninstall everything temporary, you 
can simply do

``` sh
shan config purge
```
And shan will delete everything temporarily installed. To protect you from 
accidentally purging your actual config, you can only purge your temporary one.

## Contributing

Any and all contributions are more than welcome! If you have a feature request, 
a question, or a bug to report, make an issue. If you think you can make the 
feature or fix the bug, a PR would be awesome. 

### Adding a Package Manager
shan doesn't support every package manager, but I would love it to! If you're 
interested in adding a package manager, you can request it get added in an issue,
but adding one yourself is very simple.

1. Open `src/shan/managers/managers.clj`
2. In the `package-managers` map at the top of the file, add a new entry for
   your package manager. Each value is a map, with 3 entries: `:install`, 
   `:remove`, and `:installed?`.
   1. For the `:install` value, put the command to install a package with your package manager.
   2. For the `:remove` value, put the command to remove a package.
   3. For the `:installed?` value, put the command to check if a package is locally installed.
   
Example:

```clojure
{:pip {:install "python -m pip install"
       :remove "python -m pip uninstall -y"
       :installed? "python -m pip show"}}
```

Questions? Put up an issue or make a PR with what you've got so far.

## License

The MIT License (MIT)

Copyright (c) 2021 Philip Dumaresq

