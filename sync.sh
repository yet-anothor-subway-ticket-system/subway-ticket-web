#!/bin/bash

rsync --recursive --copy-links --perms --times --delete --progress --human-readable --exclude=build --exclude=lib --exclude=*.log * root@101.200.144.204:~/subway-ticket-web
