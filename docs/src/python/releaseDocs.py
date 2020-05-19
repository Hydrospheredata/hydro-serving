#!/usr/bin/env python3
import argparse
import os
import sys
import json
import logging
import http
import http.server

class DocReleaseManager:
    def __init__(self, website_path, release_path, dry_run):
        self.website_path = website_path
        self.release_path = release_path
        self.dry_run = dry_run

    def link_release(self):
        if os.path.commonpath([os.path.abspath(self.website_path)]) == os.path.commonpath([os.path.abspath(self.website_path), os.path.abspath(self.release_path)]):
            logging.info("release is under the website folder. no link needed.")
            return
        release_name = os.path.basename(self.release_path)
        site_release_path = os.path.join(self.website_path, release_name)
        if not self.dry_run:
            if os.path.exists(site_release_path):
                real_path = os.path.realpath(site_release_path)
                logging.info("deleting existing symlink: {} -> {}".format(site_release_path, real_path))
                os.remove(site_release_path)
            os.symlink(self.release_path, site_release_path, target_is_directory=True)
        logging.info("linking release to the site folder: {} -> {}".format(self.release_path, site_release_path))

    def set_latest(self):
        latest_dir = "latest"
        root_latest_path = os.path.join(self.website_path, latest_dir)
        if not self.dry_run:
            if os.path.exists(root_latest_path):
                os.remove(root_latest_path)
            os.symlink(self.release_path, root_latest_path, target_is_directory=True)
        logging.info("linking latest {} -> {}".format(self.release_path, root_latest_path))

    def update_paradox(self):
        paradox_file = "paradox.json"
        paradox_path = os.path.join(self.release_path, paradox_file)
        root_paradox_path = os.path.join(self.website_path, paradox_file)
        if not self.dry_run:
            if os.path.exists(root_paradox_path):
                os.remove(root_paradox_path)
            os.symlink(paradox_path, root_paradox_path)
        logging.info("linking paradox.json {} -> {}".format(paradox_path, root_paradox_path))

    def update_versions(self):
        release_name = os.path.basename(self.release_path)
        ver_file = "versions.json"
        ver_path = os.path.join(self.website_path, ver_file)
        if os.path.exists(ver_path):
            with open(ver_path, "r") as f:
                ver_list = json.load(f)
                ver_list = list(set(ver_list))
            logging.info("parsing versions: {}".format(ver_list))
            if release_name not in ver_list:
                logging.info("adding version {}".format(release_name))
                ver_list.append(release_name)
            else:
                logging.info("version already exists {}".format(release_name))
            ver_list.sort(reverse=True)
        else:
            logging.info("no prior versions")
            ver_list = [release_name]
        if not self.dry_run:
            with open(ver_path, "w") as f:
                json.dump(ver_list, f)
        logging.info("writing {} -> {}".format(ver_list, ver_path))
        
    def release(self, dry_run=False):
        logging.info("releasing {} to {}".format(self.release_path, self.website_path))
        if dry_run:
            logging.warning("dry run. no changes to the filesystem.")
        self.set_latest()
        self.update_paradox()
        self.update_versions()
        self.link_release()
        logging.info("release is published to the site!")


def main(release_path, website_path, dry_run=False, server=False):
    if server:
        handler_class = http.server.partial(http.server.SimpleHTTPRequestHandler,
                                directory=website_path)
        http.server.test(HandlerClass=handler_class, port=80, bind="")
        return
    mngr = DocReleaseManager(website_path, release_path, dry_run)
    mngr.release()
    

def dir_path(path):
    if os.path.isdir(path):
        return path
    else:
        raise NotADirectoryError(path)

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, handlers=[logging.StreamHandler(sys.stdout)], format="%(asctime)s [%(levelname)s] %(message)s")
    parser = argparse.ArgumentParser()
    parser.add_argument("--release-path", type=dir_path, required=True)
    parser.add_argument("--website-path", type=dir_path, required=True)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--server", action="store_true")
    args = parser.parse_args()

    main(**vars(args))