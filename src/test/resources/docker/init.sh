#!/bin/bash
gfsh -e 'start locator --name=Locator1 --hostname-for-clients=localhost'
gfsh -e 'connect' -e 'start server --name=Server1 --hostname-for-clients=localhost'

