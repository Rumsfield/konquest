# How to Contribute to Konquest
This is a guide on the Git workflow and how to contribute to the source code.

For beginners, check out these popular workflow summaries:
* [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
* [Branching Workflows](https://git-scm.com/book/en/v2/Git-Branching-Branching-Workflows)

## Git Workflow
This repository uses the following branching convention:
1. `main` is the branch for stable, released code. New releases are tagged from here, as well as building release JARs.
2. `develop` is the branch for the latest updates, and work leading up to the next release.
3. `feature/*`, `bugfix/*` are **topic** branches which you create to work on individual tasks.

Never make direct commits to `main` and `develop`. To work on a new topic, always make a new branch from `develop`.
When the work is complete, create a new **Pull Request** to merge the topic branch back into `develop`.
Once the `develop` branch is stable and contains the desired features for a new release, it must be merged into
`main` via a new **Pull Request**. Then, `main` is tagged with the release version and built.

### External contributors
If you are not an official contributor, you can still help! Create a [fork](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
of the Konquest repository and follow the flow:
* Create a new branch from `develop`.
* Work on your changes.
* Create a new **Pull Request** to merge the branch from your repository to the base repository: `Rumsfield/konquest`, base: `develop`.

## Working on Topics
A topic is any task or update to the code. It could be a new feature or a bug fix.
Every topic must have an associated issue in the repository's [Issues page](https://github.com/Rumsfield/konquest/issues).
Each issue has an ID number, which is used to reference the topic branch name.

### Topic branch naming convention
Topic branch names must use these keywords:
* `feature` - For most topics, related to adding, removing or optimizing code.
* `bugfix` - For generally fixing broken things in the code.
* `hotfix` - For urgent errors that need to be fixed.

The name must also reference the issue number, along with a short description of the topic.
The name format is:
```
{keyword}/{issue ID}-{description-with-dashes}
```
For example:
* `feature/24-a-new-feature`
* `feature/30-big-rework`
* `bugfix/99-logic-error`
* `bugfix/112-crash-on-enable`

### Commands for working on a topic
To create a new topic branch, use these commands:
```
git checkout develop
git pull
git checkout -b {branch name}
git push origin -u {branch name}
```

Work on your changes, and commit them to your branch using some of these commands. Note:
* Use `status` to show the state of your files.
* Use `add -u` to stage all changed tracked files for commit.
* Commit as often as you like, it helps to have commits to fall back on when things go wrong.
* Push when you're ready to upload your commits to the remote server so others can view them.
```
git status
git add -u
git add {untracked file names}
git commit -m "{short message about changes}"
git push
```

Stay in sync with the `develop` branch by periodically merging it into your topic branch.
Use these commands:
```
git checkout develop
git pull
git checkout {branch name}
git merge develop
```
There may be merge conflicts during the merge. It is your respnsibility to [resolve these conflicts](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/addressing-merge-conflicts/resolving-a-merge-conflict-using-the-command-line)
and commit the resolved files. Remember to merge `develop` into your branch at least once before opening a Pull Request.

When your work is finished, create a new [Pull Request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request)
to [Squash and Merge](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/incorporating-changes-from-a-pull-request/about-pull-request-merges#squash-and-merge-your-commits) 
into the `develop` branch.
* **DO NOT** merge your topic branch directly into `develop` or `main` using the `git merge` command.
* **DO NOT** commit directly in `develop` or `main` using the `git commit` command.
* **ALWAYS** use Pull Requests to merge topic branches back into `develop` using **Squash and Merge**.

Once the Pull Request is merged and closed, **please delete** the topic branch to clean up the repo.

## Preparing Releases
Usually the latest work on the `develop` branch becomes the next release.
Planning for the content of a release is done in an issue, by tracking which topic issues to include.
Use the ID of the issue in the branch name, using one of these keywords:
* `release` - For new release versions that end in 0, i.e. vX.Y.0.
* `patch` - For versions that increment the patch number, i.e. vX.Y.1.

For example,
* `release/101-v1.1.0`
* `patch/120-v1.1.1`

### Release versioning
Release versions follow a MAJOR.MINOR.PATCH format. The project version is defined in [build.gradle.kts](https://github.com/Rumsfield/konquest/blob/main/build.gradle.kts).
* PATCH - Increment when updates are mostly bug fixes and small improvements.
* MINOR - Increment when there are large updates like new features, changes to commands, configuration, etc.
* MAJOR - Increment when there are major wide-spread changes and incompatible API changes (and when the project moves from private beta to public open-source release).

### How to make a release branch
Create a branch for the new release or patch from `develop`, just like a topic branch.
The `develop` branch should have all the latest features that you want to include in the release.
In the release branch, 
* Update the project version in `build.gradle.kts`.
* Re-generate the Javadoc if there were API-related changes or if the Major or Minor version changes.
* Do a final play test of a plugin build in a fresh server, and test all the new features.

### Steps to publish a new release
Once the release branch is ready and contains every desired feature of the new version, follow these steps to publish it.

1. Create a new Pull Request to merge the release branch into `develop` using **Squash and Merge**.
2. Once `develop` has the new release commit, create a second Pull Request to merge `develop` into `main` using **Merge Commit**.
3. Once the merge into `main` is complete, create a new tag in `main` using the release version as a name.
```
git checkout main
git pull
git tag -a vX.Y.Z -m "{description}"
git push origin vX.Y.Z
```
4. Clean and build the project from `main` to get JAR files.
```
./gradlew clean
./gradlew shadowJar
```
5. On GitHub, [draft a new release](https://github.com/Rumsfield/konquest/releases) using the new tag. Provide detailed change notes and upload the JAR files as release assets:
    * Konquest-X.Y.Z.jar
    * konquest-api-X.Y.Z.jar