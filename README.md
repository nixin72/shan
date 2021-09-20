**SUPER EARLY STAGES. NOT USABLE, AND FAR FROM STABLE**

# Shan

A declarative wrapper for your favourite package manager.

## Usage

### Add or remove packages with your config
You can use the `shan.edn` file to maintain a list of all the packages you want
installed. Adding or removing packages, then running `shan sync` will sync the 
configuration file to your list of installed packages. Packages installed via 
other means will not be effected, the only packages that will be removed are the
ones that have been removed from your `shan.edn`.

1. Open your `~/.config/shan.edn`
2. Add or remove a package 
3. Run `shan sync`

### Install right from command line
Editing the config and then syncing every time you want to install something can
be a pain in the ass. As an alternative, you can install something right from 
the command line and add it to your config in one command:

```sh
shan i npm react-native
```

You can also omit the package manager if you're installing a package for the 
first package manager in your config:

Config:
```clojure
{:yay
  [vim zsh leiningen]}
```
Then run 
```sh
shan i clang
```

Will install clang and produce
```clojure
{:yay
  [vim zsh leiningen clang]}
```

### Removing from the command line
Like installing, you can also remove packages right from the command line. This 
is a little easier, since you don't need to specify the package manager, it'll 
just look for a package with the same name in your config.

```
shan rm clang
```

Will revert our config back to
```clojure
{:yay
  [vim zsh leiningen]}
```

If there's packages from two package managers under the same name, you can 
either specify which one you want to be removed, or if you omit it, it'll prompt
you to select which one you want deleted.

```clojure
{:yay
  [react-native-cli]
 :npm
  [react-native-cli]}
```

```
$ shan rm react-native-cli
You have multiple packages installed named `react-native-cli`. Which one would
you like to remove?
1) yay 2) npm
```

### Editing your config
The configuration for shan is stored in `~/.config/shan.edn`. The format for this 
file follows Clojure's EDN data format. This format is very similar to JSON, 
however `:`s come before the keys and commas are unnecessary.

``` clojure
{:this "is a hashmap"
 :and-this ["is" "an" "array"]
 :and-now-this #{"is" "a" "set"}}
```

You shan config is a simple structure with a hashmap containing arrays. Each key 
in the hashmap is a supported package manager (such as `:yay` or `:npm`), and 
the values are an array of strings, where each string is a package name.

### Temporary packages
Sometimes you want to quickly install a package to test it out, but you don't 
necessarily want it to be added to your config. But then you forget to uninstall
it later, and these random packages accumulate. With shan, you can install a
package temporarily, and shan will keep track of all the temporary packages to
allow you to list what you have temporarily installed so you can add them to 
your config, or so you can purge all the temporary packages.

```sh
$ shan ti yay btop htop
$ shan tls
btop
htop
$ shan trm btop
$ shan tls
htop
$ shan tp
$ shan tls
No temporary packages installed
```



