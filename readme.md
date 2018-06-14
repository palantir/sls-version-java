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

For any two orderable versions, v1 and v2, we can define whether v1 is a *bigger* (equivalently, *later*, *newer*, etc)
than v2. For the four variants, there can be up to three numeric components identifying a version. From left to right,
they are: the usual notation of the base version (e.g., for `1.2.3`, 1=major, 2=minor, 3=patch), an optional second
numeric component to identify a release candidate (e.g. `-rc3`) or a snapshot version (e.g. `-5-gnm4s9ba`), and finally
an optional third numeric component to identify a release candidate snapshot version (e.g. `-rc3-5-gnm4s9ba`).

Intuitively, given the same base version, snapshot versions are bigger than non-snapshot versions, normal release
versions are bigger than release candidate versions, and a normal release snapshot version is bigger than a release
candidate of any kind. The following top-down procedure determines whether v1 is bigger than v2, written `v1 > v2`;
comparisons like `major(v1) > major(v2)` are by integer ordering (not lexicographic ordering):

- If `major(v1) > major(v2)`, then `v1 > v2`
- If `minor(v1) > minor(v2)`, then `v1 > v2`
- If `patch(v1) > patch(v2)`, then `v1 > v2`
- From here on, let us assume that the base versions (major/minor/patch) are the same for v1 and v2
- If v1 is a normal snapshot version and v2 is a normal release, then `v1 > v2`
- If v1 is a normal release version and v2 is a rc version, then `v1 > v2`
- If v1 and v2 are both normal snapshot versions and `snapshot(v1) > snapshot(v2)`, then `v1 > v2`
- If v1 and v2 are both rc versions and `rc(v1) > rc(v2)`, then `v1 > v2`
- From here on, let us assume that v1 and v2 are both rc versions of the same `rc()` number
- If v1 is a snapshot rc version and v2 is a normal rc version, then `v1 > v2`
- If v1 and v2 are both snapshot rc versions and `rcSnapshot(v1) > rcSnapshot(v2)`, then `v1 > v2`

Further, v1 is as big as v2, written `v1 == v2`, iff neither `v1 > v2` nor `v2 > v1`.
We write `v1 >= v2` if `v1 > v2` or `v1 == v2`.

Examples, with each greater than all the previous:
- RC: `1.0.0-rc1`
- Bigger RC: `1.0.0-rc2`
- RC Snapshot trumps RC: `1.0.0-rc2-4-gaaaaaaa`
- Bigger RC Snapshot: `1.0.0-rc2-5-gccccccc`
- Base trumps RC: `2.0.0`
- Snapshot trumps all: `2.0.0-3-gaaaaaaa`
- Bigger Snapshot: `2.0.0-4-gbbbbbbb`
- Bigger Base: `2.1.0-rc1`
- Release trumps RC: `2.1.0`

Examples of equality:
- `1.2.0 == 1.2.0`
- `2.0.0-rc1 == 2.0.0-rc1`
- `2.0.0-rc1-3-gaaaaaaa == 2.0.0-rc1-3-gbbbbbbb`
- `2.0.0-5-gbbbbbbb == 2.0.0-5-gaaaaaaa1`

Note that any two release and rc versions are equally big iff they are syntactically equal. As the second example
demonstrates, this does not hold for snapshot versions.


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

