from runtime import RuntimeManager

import os
import time
import logging

logging.basicConfig(level=logging.INFO)

if __name__ == '__main__':
    runtime = RuntimeManager('/model', port=int(os.getenv('APP_PORT', "9090")))
    runtime.start()

    try:
        while True:
            time.sleep(60 * 60 * 24)
    except KeyboardInterrupt:
        runtime.stop()