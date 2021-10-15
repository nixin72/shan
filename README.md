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

- [ ] dpkg
- [x] yay
- [x] paru
- [ ] dnf
- [x] brew
- [ ] chocolatey
- [ ] winget
- [x] npm
- [x] pip
- [x] Gems

## Table of Contents

- [Why use shan?](#why-shan)
- [Using shan](#usage)
  - [Installing packages](#installing-packages)
  - [Default package manager](#default-package-manager)
  - [Removing packages](#removing-packages)
  - [Listing packages](#listing-packages)
  - [Temporarily installed packages](#temporary-packages)
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

All the commands available at the moment are:
install, add-repo
remove, del-repo
sync, rollback
edit, default
purge, merge
list, gen

### Installing packages

Installing packages with shan is easily done through the `install` or `in`
subcommand.

```sh
shan install nodejs
```

For example will install `nodejs` using the [default package manager](#your-config).

You can also specify to shan which package manager should be used to install a
package:

```sh
shan install nodejs -npm react
```

And this will install `nodejs` using the default package manager, and then `react`
using npm.

You can also mix and match things in any order and install as many packages as you
want in a single command:

```sh
shan install nodejs python -npm react react-native -pip PyYAML wakatime -npm expo
```

Every time you install a package, the package will also be added to
[your config file](#your-config) so that down the road, you'll have everything
available to you in a declarative config.

### Package versions

For most package managers, you can specify which version of a package you want
when installing a package:

```sh
$ shan install -npm react=17.0.2 -pip wakatime=13.1.0
```

### Default package manager

Most people probably don't want to need to specify which package manager to use
every time they want to install something. Most people also probably use one
package manager more often than others, like `yay` or `brew`. For these
purposes, shan allows you to set a default package manager to use.

```sh
shan default yay
```

This will set `yay` to be the default package manager that shan should use. Now,
when you install a package using `shan install <package>`, you don't need to tell
shan that it should use `yay` to install it, shan will use the default.

### Removing packages

When removing a package using Shan, it's generally able to figure out which
package manager it should be using to remove a package. Let's say we have the
following packages installed:

```clojure
yay
nodejs neovim atop

npm
react underscore atop
```

Now, if we want to remove `nodejs`, we simply have to do:

```sh
shan remove nodejs
```

And shan will figure out which package manager to remove nodejs using. Unlike
installing, where it'll install using your default package manager, removing will
simply remove from where the package exists.

```sh
shan remove nodejs neovim react
```

Will remove `nodejs` and `neovim` using yay, and `react` will be removed using
`npm`.

We do however have `atop` installed here in both yay _and_ npm. In this case,
Shan will prompt you for which package manager you want to uninstall `atop` from.

You can also specify instead if you'd like

```sh
$ shan rm -yay atop
```

But for convenience sake, you don't have to.

A package also doesn't need to be in your config to uninstall it.

```sh
shan rm emacs
```

If you remove a package that isn't in your config, shan will look at which
package mangers are available on your system, then check if that package is
installed using any of those package managers. If it is, then it'll remove the
package using that package manager, and let you know where it found it.

### Listing packages

Knowing which packages you have installed and being able to easily search for
them is important. That's why shan provides utilities for listing everything
you have installed in a variety of ways.

```sh
$ shan list
yay
neovim nodejs emacs

npm
react-native expo-cli

pip
wakatime
```

You can also add flags to the `list` command to get the data in a variety of formats.

```sh
$ shan list --json
{
    "yay": ["neovim", "nodejs", "emacs"],
    "npm": ["react-native", "expo-cli"],
    "pip": ["wakatime"]
}
$ shan list --parse
yay neovim
yay nodejs
yay emacs
npm react-native
npm expo-cli
pip wakatime
```

### Temporary packages

Sometimes you want to quickly install a package to test it out, but you don't
necessarily want it to be added to your config. When installing or removing a
package, shan supports the ability to make "temporary" changes. This means that
the changes you make won't get added to your config file.

```sh
shan install -t nodejs -npm react
```

Will install nodejs using the default package manager and react using npm, however
neither of these packages will get added to your config. Instead, the changes will
be saved in a separate file. This way, shan can act against that file independently
of your config. For example if you want to uninstall everything temporary, you
can simply do

```sh
shan purge
```

And shan will delete everything temporarily installed. To protect you from
accidentally purging your actual config, you can only purge your temporary one.

In addition to purging your temporary files, you can also _merge_ your list of
temporary packages with your config. This will take all packages installed
temporarily and add them to your `shan.edn` file.

```sh
shan merge
```

## Contributing

Any and all contributions are more than welcome! If you have a feature request,
a question, or a bug to report, make an issue. If you think you can make the
feature or fix the bug, a PR would be awesome.

### Adding a Package Manager

shan doesn't support every package manager, but I would love it to! If you're
interested in adding a package manager, you can request it get added in an issue.

If you'd like to take a shot at adding one yourself, it's very simple though.

1. Open `src/shan/managers.clj`
2. In the `package-managers` map at the top of the file, add a new entry for
   your package manager. Each value is a map, with at least 3 entries:
   `:install`, `:remove`, `:installed?`.

   1. For the `:install` value, put the command to install a package with your
      package manager.
   2. For the `:remove` value, put the command to remove a package.
   3. For the `:installed?` value, put the command to check if a package is
      locally installed.

   There are other values that can and should be added if you want to get full
   usage out of the given package manager. Other supported keys are `:type`,
   `:list`, `:add-archive`, `:remove-archive`, `pre-install`.

   1. The `:type` key should be what type of package manager it is. Possible
      values are `:system` and `:language`. The default is `:system`.
   2. `:list` is a little complicated. It should be present for all package
      managers, but it's not required. If you don't know Clojure, feel free to
      leave this one to the maintainers. If you do know it, take a look at the
      `src/shan/managers/list.clj` file. The value for `:list` should be a
      function that gets a list of all packages installed via that package
      manager, and creates a list of all of them.
   3. The `:add-archive` and `:remove-archive` keys are strings that contain the
      command to add a new package archive for the package manager. Not all
      package managers have package archives, so this isn't necessary for some.
   4. `:pre-install` is something that will run before any packages get
      installed using that package manager. This is useful for doing things like
      updating the package archives so that packages you want that aren't in the
      default archives become available.

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
