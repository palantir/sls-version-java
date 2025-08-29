<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/sls-version-java"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

# SLS Versions
[![Build Status](https://circleci.com/gh/palantir/sls-version-java.svg?style=shield)](https://circleci.com/gh/palantir/sls-version-java)
[![Download](https://api.bintray.com/packages/palantir/releases/sls-version-java/images/download.svg)](https://bintray.com/palantir/releases/sls-version-java/_latestVersion)

A collection of useful Java classes for dealing with the SLS versions according to the [SLS Product Version Specification](#sls-product-version-specification).

```gradle
dependencies {
    compile 'com.palantir.sls.versions:sls-versions:<version>'
}
```

### Overview

Useful classes under `com.palantir.sls.versions`:

* `SlsVersion` - a class hierarchy that represents valid SLS versions
  * `OrderableSlsVersion`
  * `NonOrderableSlsVersion`
* `SlsVersionMatcher` - a matcher than supports matching specific ranges of versions, for example:
  * `1.2.3` matches only the version `1.2.3`
  * `1.2.x` matches any SLS version that starts with `1.2.`
  * `1.x.x` matches any SLS version that starts with `1.`
* `VersionComparator` - allows comparing pairs of `OrderableSlsVersion` to determine which is newer

# SLS Product Version Specification

This specification describes _orderable_ and _non-orderable_ product version strings for use in an SLS manifest.

### Orderable version strings

Orderable version strings fall into one of 4 version types as defined by a category (release or release candidate)
and whether it is a snapshot version or not (snapshot versions contain a commit hash at the end), the cross section
of which produces the following:

 ```
Version Type                        Example                 Format
------------                        -------                 ------
Release                             1.0.0                   ^[0-9]+\.[0-9]+\.[0-9]+$
Release Snapshot                    1.0.0-1-gaaaaaaa        ^[0-9]+\.[0-9]+\.[0-9]+-[0-9]+-g[a-f0-9]+$

Release candidate (rc)              1.0.0-rc1               ^[0-9]+\.[0-9]+\.[0-9]+-rc[0-9]+$
Release candidate (rc) snapshot     1.0.0-rc1-1-gaaaaaaa    ^[0-9]+\.[0-9]+\.[0-9]+-rc[0-9]+-[0-9]+-g[a-f0-9]+$
```

Note that in each example above, we define the 'base' version as the  major/minor/patch-version component (in this
case, they are all the same, `1.0.0`).


#### Ordering

For any pair of orderable product version strings, it is straightforward to define an order governing which of the
product versions is newer and which one is older. The order allows deployment tooling to make informed
decisions about forward-vs-backwards product migrations. Further, it simplifies reasoning about product compatibility
via version ranges; for instance, a product may declare that it is compatible with a second product with a version in
`[1.2.3, 2.0.0)`.

For any two orderable versions, `v1` and `v2`, we can define whether `v1` is a *larger* (equivalently, *later*, *newer*, etc)
than `v2`. For the four variants, there can be up to three numeric components identifying a version. From left to right,
they are: the usual notation of the base version (e.g., for `1.2.3`, 1=major, 2=minor, 3=patch), an optional first
numeric component to identify a release candidate (e.g. `-rc3`) or a snapshot version (e.g. `-5-gnm4s9ba`), and finally
an optional second numeric component to identify a release candidate snapshot version (e.g. `-rc3-5-gnm4s9ba`).

Intuitively, the version types define an ordering of versions for a given base version:
- For a given base version, release snapshot versions are larger than release versions
- For a given base version, release versions are larger than release candidate and release candidate snapshot versions
- For a given base and rc version, release candidate snapshot versions are larger than release candidate versions

The following top-down procedure determines whether `v1` is larger than `v2`, written `v1 > v2`;
comparisons like `major(v1) > major(v2)` are by integer ordering (not lexicographic ordering):

- We know nothing about `v1` and `v2`:
  - If `major(v1) > major(v2)`, then `v1 > v2`
  - If `minor(v1) > minor(v2)`, then `v1 > v2`
  - If `patch(v1) > patch(v2)`, then `v1 > v2`
- From here on, we know that `v1` and `v2` have the same base version:
  - If both `v1` and `v2` are both release snapshot versions and `snapshot(v1) > snapshot(v2)`, then `v1 > v2`
  - If `v1` is a release snapshot version and `v2` is not, then `v1 > v2`
  - If both `v1` and `v2` are both release versions, then `v1 == v2`
  - If `v1` is a release version and `v2` is not, then `v1 > v2`
- From here on, we know that `v1` and `v2` have the same base version and are release candidate or release candidate snapshot version:
  - If `rc(v1) > rc(v2)`, then `v1 > v2`
  - If `v1` is a release candidate snapshot version and `v2` is not, then `v1 > v2`
  - If `rcSnapshot(v1) > rcSnapshot(v2)`, then `v1 > v2`

Examples, with each greater than all the previous:
- `1.0.0-rc1`
- `1.0.0-rc1-1-gabcedf`
- `1.0.0-rc1-2-gabcedf`
- `1.0.0-rc2`
- `1.0.0`
- `1.0.0-1-gabcedf`
- `1.0.0-2-gabcedf`
- `1.0.1-rc1`
- `1.0.1`
- `1.1.0`
- `2.0.0`

Examples of equality:
- `1.0.0-rc1 == 1.0.0-rc1`
- `1.0.0-rc1-1-gaaaaaaa == 1.0.0-rc1-1-gbbbbbbb`
- `1.0.0 == 1.0.0`
- `1.0.0-1-gaaaaaaa == 1.0.0-1-gbbbbbbb`

#### Version matchers

A *version matcher* is a specification of a set of *release versions* and defined by the regular expression:

```
^((x\.x\.x)|([0-9]+\.x\.x)|([0-9]+\.[0-9]+\.x)|([0-9]+\.[0-9]+\.[0-9]+))$
```

The serialized form of a version matcher is a string that matches this regular expression. For example, `1.x.x`,
`2.0.x`, `x.x.x`, and `1.2.3` are valid version matchers, whereas `x.y.z`, `x.0.0`, `0.x.3`, `x.x.2`,  `1.x`, and
`^x\.[0-9]+\.[0-9]+$` are not. A matcher is said to *match* a release version if there are (independent) substitutions
for `x` that turn the matcher into the version. For example, `1.x.x` matches `1.0.0` and `1.2.3`, but it does not match
`2.0.0` or `0.1.1`.

### Non-orderable version strings

Version strings follow the *non-orderable* format if they match the follow regular expression:
```
^[0-9]+\.[0-9]+\.[0-9]+(-[a-z0-9-]+)?(\.dirty)?$
```
For example, `1.0.0.dirty`, `0.0.1-custom-description-42`, and `2.0.0-1-gaaaaaa.dirty` are valid but non-orderable version
strings, whereas `5.0`, `1.1.2.3-foo`, `1.1.2.3` and `1.0.0-FOO` are not valid version strings under this spec.

There are no ordering guarantees for a pair of non-orderable versions, or between an orderable and a non-orderable version string.

