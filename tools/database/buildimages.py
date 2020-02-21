#!/usr/bin/python
# -*- coding: utf-8 -*-
import shutil

imgpack_filename = 'images.zip'
            
shutil.copyfile(imgpack_filename, '../../WikiPicsTrivia/src/main/res/raw/' + imgpack_filename);