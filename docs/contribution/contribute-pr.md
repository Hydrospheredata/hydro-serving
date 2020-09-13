# Contributing Pull Requests

This guide is written for contributing to documentation. It doesn't contain any instructions on installing software prerequisites. If your intended contribution requires any software installations, please refer to their respective official documentation.

**Prerequisites**

* Git installed on your local machine
* GitHub account 

**Contents**

1. PR Contribution Workflow   
2. Basic Workflow Example  
3. PR Acceptance policy  

### PR Contribution Workflow

1. Fork and clone this repository \(`git clone`\)  
2. Create a feature branch against master \(`git checkout -b featurename`\)   
3. Make changes in the feature branch  
4. Use linters to ensure correct syntax and formatting \(`terraform fmt`, `pre-commit run -a`\)  
5. Commit your changes \(`git commit -am "Add a feature"`\)   
6. Push your changes to GitHub \(`git push origin feature`\)    
7. Open a Pull Request and wait for your PR to get reviewed   
8. Edit your PR to address feedback \(if any\)   
9. See your PR getting merged  

#### 1. Fork and Clone this Repository

In order to contribute, you need to make your own copy of the repository you're going to contribute to. You do this by forking the repository to your GitHub account and then cloning the fork to your local machine.

1. Fork this GitHub repository: on GitHub, navigate to the [main page of the repository](https://github.com/Hydrospheredata/hydro-serving) and click the Fork button in the upper-right area of the screen. This will create a fork \(a copy of this repository in your GitHub account\). 
2. Clone the fork and switch to the project directory by running in your terminal:

   ```text
   git clone https://github.com/Hydrospheredata/hydro-serving.git
   cd hydro-serving
   ```

   **2. Create a New Branch**

   It is important to make all your changes in a separate branch created off the master branch. 

   Before any modifications to the repository that you've just cloned, create a new branch off of the master branch. 

Create a new branch off of the current one and switch to it:

```text
git checkout -b <your-branch-name>
```

To switch between branches, use the same command without the `-b` flag. For example, to switch back to the master branch:

```text
git checkout master
```

This way you can switch between multiple branches when you work on multiple features at once.

**Branch Naming Conventions**

Give your branch a descriptive name so that others working on the project understand what you are working on. The branch name should include the name of the module that you're contributing to.

Name your branch according to the following template, replacing `nginx` with the name of the module you're contributing to:

```text
feature/docs_nginx
```

#### 3. Make Changes

Make changes you want to propose. Make sure you do this in a dedicated branch based on the master branch.

#### 4. Use Linter for Correct Syntax & Formatting

When applicable, use linters to ensure proper formatting before committing and pushing your changes. 

#### 5. Commit Changes

Commit changes often to avoid accidental data loss. Make sure to provide your commits with descriptive comments.

```text
git add .
git commit -m "Add description"
```

Or add and commit all changed files with one command:

```text
git commit -am "Add description"
```

#### 6. Push Changes to GitHub

Push your local changes to your fork on GitHub.

```text
git push <repo-name> <branch-name>
```

For example, if your remote repository is called origin and you want to push a branch named docs/fix:

```text
git push origin docs/fix
```

#### 7. Open a Pull Request

Navigate to your fork on GitHub. Press the "New pull request" button in the upper-left part of the page. Add a title and a comment. Once you press the "Create pull request" button, the maintainers of this repository will receive your PR.

#### 8. Address Feedback

After you submit the PR, one or several of the Hydrosphere repository reviewers will provide you with actionable feedback. Edit your PR to address all of the comments. Reviewers do their best to provide feedback and approval in a timely fashion but note that response time may vary based on circumstances.

#### 9. Your PR Gets Merged

Once your PR is approved by a reviewer, it gets accepted and merged with the main repository. Merged PRs will get included in the next Hydrosphere release.

### Basic Workflow Example

```text
git clone https://github.com/Hydrospheredata/hydro-serving.git
cd hydro-serving
git checkout -b docs/fix
git status
git commit -am "Add description"
git push origin docs/fix
```

### PR Acceptance Policy

What will make your PR more likely to get accepted:

* Having your fixes on a dedicated branch 
* Proper branch naming
* Descriptive commit messages
* PR title describing what changed 
* PR comment describing why/where it changed in &lt;80 chars
* Texts checked for spelling and typos \(you can use Grammarly\)
* Code snippets checked with linters \(when applicable\)  

#### PR Title and Comment Conventions

A PR title should describe what has changed. A PR comment should describe why and what/where. If your changes relate to a particular issue, a PR comment should contain an issue number. Please keep PR comments below 80 characters for readability.

PR title example:

```text
Updated docs: README.md and CONTRIBUTING.md.
```

PR comment example:

```text
Updated README.md (minor changes, fixed all typos). 
Updated CONTRIBUTING.md (added a paragraph about 
using linters, added sections: 
"Use Linter to ensure correct syntax and formatting", 
"Push your changes to GitHub" to close issue 42. 

Issue #42
```

Minor edits \(typos, spelling, formatting, adding small text pieces\) may get waved through. More substantial changes normally require more time, reviewers, and back-and-forths, and you might get asked for a PR resubmission or dividing changes into more that one PR. Usually, PRs are getting merged right after the approval.

