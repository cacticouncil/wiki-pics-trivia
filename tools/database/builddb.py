#!/usr/bin/python
# -*- coding: utf-8 -*-
import shutil
import sqlite3
import os

data_filename = 'data.sql'

# Convert file wikipicstrivia.db to SQL dump file data.sql
con = sqlite3.connect('wikipicstrivia.db')
with open(data_filename, 'w') as f:
    for line in con.iterdump():
        if line.startswith('INSERT'):
            f.write(('%s\n' % line).encode('utf-8'))
            
shutil.copyfile(data_filename, '../../WikiPicsTrivia/src/main/res/raw/' + data_filename);