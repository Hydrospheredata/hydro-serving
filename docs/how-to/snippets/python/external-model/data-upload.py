from argparse import ArgumentParser
from urllib.parse import urljoin

import requests


def read_in_chunks(filename, chunk_size=1024):
    """ Generator to read a file peace by peace. """
    with open(filename, "rb") as file:
        while True:
            data = file.read(chunk_size)
            if not data:
                break
            yield data


if __name__ == "__main__": 
    parser = ArgumentParser()
    parser.add_argument("--hydrosphere", type=str, required=True)
    parser.add_argument("--model-version-id", type=int, required=True)
    parser.add_argument("--filename", required=True)
    parser.add_argument("--chunk-size", default=1024)
    args, unknown = parser.parse_known_args()
    if unknown:
        print("Parsed unknown arguments: %s", unknown)

    endpoint_uri = "/monitoring/profiles/batch/{}".format(args.model_version_id)
    endpoint_uri = urljoin(args.hydrosphere, endpoint_uri) 
    
    gen = read_in_chunks(args.filename, chunk_size=args.chunk_size)
    response = requests.post(endpoint_uri, data=gen, stream=True)
    if response.status_code != 200:
        print("Got error:", response.text)
    else:
        print("Uploaded data:", response.text)
