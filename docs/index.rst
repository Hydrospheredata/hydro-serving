Welcome to Hydrosphere's documentation!
=======================================

Eager to get started? Soon we will publish a more detailed documentation, 
but for now you can check out :ref:`trouble-shooting` section. 


.. _trouble-shooting:

Trouble Shooting
----------------

Sometimes there might occur some weird problems while working with 
``hydro-serving``. Down below listed some workarounds that may help you 
to resolve your issues. If those do not help, you can ask a question
via gitter.

Connection Refused Error
^^^^^^^^^^^^^^^^^^^^^^^^
  When working with ``hydro-serving`` in your local environment you may
  encounter some weird connection error, when uploading model via cli-tool ``hs upload``::

    Failed to establish a new connection: [Errno 61] Connection refused.

  This can be resolved by stopping your docker instance
  and rebuilding existing containters::

    $ docker stop $(docker ps -aq)
    $ docker rm $(docker ps -aq)
    $ docker-compose up
