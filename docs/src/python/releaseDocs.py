#!/usr/bin/env python3
import argparse
import os
import sys
import json
import logging
import http
import http.server
import re
import shutil
import itertools
from collections import defaultdict, namedtuple


###############################################################################
# SEMVER HELPERS                                                              #
###############################################################################

Version = namedtuple("Version", ["major", "minor", "patch", "prerelease", "build"])

#: Regex for a semver version
# Courtesy of https://github.com/python-semver/python-semver
SEMVER_REGEX = re.compile(
    r"""
        ^
        (?P<major>0|[1-9]\d*)
        \.
        (?P<minor>0|[1-9]\d*)
        \.
        (?P<patch>0|[1-9]\d*)
        (?:-(?P<prerelease>
            (?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)
            (?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*
        ))?
        (?:\+(?P<build>
            [0-9a-zA-Z-]+
            (?:\.[0-9a-zA-Z-]+)*
        ))?
        $
    """,
    re.VERBOSE,
)

def parse_version(release_name):
    match = SEMVER_REGEX.match(release_name)
    if match is None:
        logging.warning("{} is not a semantic version.".format(release_name))
        return None
    version_parts = match.groupdict()
    version_parts["major"] = int(version_parts["major"])
    version_parts["minor"] = int(version_parts["minor"])
    version_parts["patch"] = int(version_parts["patch"])
    return Version(version_parts["major"], version_parts["minor"], version_parts["patch"], version_parts["prerelease"], version_parts["build"])


def version_to_string(version):
    fmt_str = "{}.{}.{}".format(version.major, version.minor, version.patch)
    if version.prerelease:
      fmt_str =  "{}-{}".format(fmt_str, version[3])
    if version.build:
      fmt_str =  "{}+{}".format(fmt_str, version[4])
    return fmt_str

# Patch string representation for beautiful output
Version.__repr__ = version_to_string

def cleanup_versions(all_versions):
    parsed = defaultdict(lambda: [])

    versions = []
    nonsemverions = []
    to_delete = []
    latest = None
    
    for x in all_versions:
        p = parse_version(x)
        if not p:
            nonsemverions.append(x)
            continue
        parsed[p[:2]].append(p)

    for v in parsed.values():
        v.sort(reverse=True)
        head, *tail = v
        logging.debug("leaving {}. deleting {}".format(head, tail))
        versions.append(head)
        to_delete = to_delete + tail
        if not latest:
            latest = head
        elif head.major > latest.major:
            latest = head
        elif head.minor > latest.minor:
            latest = head
    versions.sort(reverse=True)
    versions = versions + nonsemverions
    return (latest, versions, to_delete)

###############################################################################
# RELEASE PROCESS                                                             #
###############################################################################

class DocReleaseManager:
    VERSIONS_FILE = "versions.json"
    RELEASE_INFO_FILE = "paradox.json"

    def __init__(self, website_path, dry_run):
        self.website_path = website_path
        self.versions_path = os.path.join(self.website_path, self.VERSIONS_FILE)
        self.paradox_path = os.path.join(self.website_path, self.RELEASE_INFO_FILE)

        self.execute = not dry_run

    def get_versions(self):
        try:
            with open(self.versions_path) as f:
                ver_list = json.load(f)
                ver_list = set(ver_list)
                logging.info("previous versions: {}".format(ver_list))
                return ver_list
        except IOError:
            logging.warning("couldn't open {} file. assuming no previous versions.".format(self.versions_path))
            return set()
            
    def copy_release(self, release_path):
        if os.path.commonpath([os.path.abspath(self.website_path)]) == os.path.commonpath([os.path.abspath(self.website_path), os.path.abspath(release_path)]):
            logging.info("release is under the website folder. no copy needed.")
            return
        release_name = os.path.basename(release_path)
        site_release_path = os.path.join(self.website_path, release_name)
        if os.path.exists(site_release_path):
            logging.info("deleting directory: {}".format(site_release_path))
            if self.execute:
                shutil.rmtree(site_release_path)
        logging.info("copying directory: {} -> {}".format(release_path, site_release_path))
        if self.execute:
            shutil.copytree(release_path, site_release_path)

    def update_paradox(self, latest_version):
        root_latest_path = os.path.join(self.website_path, str(latest_version))
        paradox_file = "paradox.json"
        paradox_path = os.path.join(root_latest_path, paradox_file)
        root_paradox_path = os.path.join(self.website_path, paradox_file)
        if self.execute:
            if os.path.exists(root_paradox_path):
                os.remove(root_paradox_path)
            os.symlink(paradox_path, root_paradox_path)
        logging.info("linking paradox.json {} -> {}".format(paradox_path, root_paradox_path))

    def update_latest(self, latest_version):
        latest_dir = "latest"
        fullpath = os.path.join(self.website_path, str(latest_version))
        root_latest_path = os.path.join(self.website_path, latest_dir)
        if self.execute:
            if os.path.exists(root_latest_path):
                os.remove(root_latest_path)
            os.symlink(fullpath, root_latest_path, target_is_directory=True)
        logging.info("linking latest {} -> {}".format(fullpath, root_latest_path))
        self.update_paradox(latest_version)

    def update_versions(self, versions):
        str_versions = [str(x) for x in versions]
        logging.info("wrote versions to file: {}".format(self.versions_path))
        if self.execute:
            with open(self.versions_path, "w") as f:
                json.dump(str_versions, f)

    def delete_version(self, version):
        str_version = version_to_string(version)
        fullpath = os.path.join(self.website_path, str_version)
        if os.path.exists(fullpath):
            logging.info("deleted version {}: {}".format(str_version, fullpath))
            if self.execute:
                shutil.rmtree(fullpath)
        else:
            logging.error("version {} doesn't exist: {}".format(str_version, fullpath))

    def release(self, release_version, dry_run=False):
        release_path = os.path.join(self.website_path, release_version)
        if not os.path.isdir(release_path):
            raise ValueError("release folder is not found: {}".format(release_path))
        logging.info("releasing {}".format(release_path))

        all_versions = self.get_versions()
        all_versions.add(release_version)

        if dry_run:
            logging.warning("dry run. no changes to the filesystem.")
        (latest, versions, to_delete) = cleanup_versions(all_versions)
        if not latest:
            latest = release_version
        logging.info("versions: {}".format(versions))
        logging.info("latest: {}".format(latest))
        self.update_latest(latest)
        self.update_versions(versions)
        
        if to_delete:
            for d in to_delete:
                self.delete_version(d)

        logging.info("release is published to the site!")

###############################################################################
# CLI                                                                         #
###############################################################################

def main(website_path, release_version, dry_run=False, server=False):
    if server:
        handler_class = http.server.partial(http.server.SimpleHTTPRequestHandler,
                                directory=website_path)
        http.server.test(HandlerClass=handler_class, port=80, bind="")
        return
    mngr = DocReleaseManager(website_path, dry_run)
    mngr.release(release_version)

def dir_path(path):
    if os.path.isdir(path):
        return path
    else:
        raise NotADirectoryError(path)

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, handlers=[logging.StreamHandler(sys.stdout)], format="%(asctime)s [%(levelname)s] %(message)s")
    parser = argparse.ArgumentParser()
    parser.add_argument("--website-path", type=dir_path, required=True)
    parser.add_argument("--release-version", type=str, required=True)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--server", action="store_true")
    args = parser.parse_args()

    main(**vars(args))