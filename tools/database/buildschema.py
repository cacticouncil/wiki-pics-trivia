#!/usr/bin/python
# -*- coding: utf-8 -*-
'''
@author: Joe Del Rocco
@author: Arian Sardari
@since: 06/28/2014
@program: Generate WikiPicsTrivia DB given schema and data.
'''
import sys
import os
import argparse
import csv
import shutil


class GlobalInfo:
    def __init__(self):
        self.args = None
        self.props = {}
        self.sqlthemes = []
        self.sqlcats = []
        self.sqlctop = []
        self.datafile = None
        self.themecats = []
        self.sqlmodes = []
        self.sqlstats = []
GLOBALS = GlobalInfo()


def process_settings():
    global GLOBALS;

    # open file
    ifile = open(GLOBALS.args.sett, 'rU')
    csvreader = csv.reader(ifile, quotechar='"', delimiter=',')
    row = csvreader.next()  # ignore header

    # init
    pkey = 0;

    # process data
    for row in csvreader:
        if (" " in row[0]):
            print "Warning: invalid property '" + row[0] + "'";
            continue;
        row[1] = row[1].replace("'", "''");  # escape single quotes for proper SQL
        GLOBALS.datafile.write("INSERT INTO settings (lang, sfx, dnr, learning, showAnswer, updates, increment, help, qotd, qotd_hour, qotd_minute, music) values ('" +
            str(row[0]) + "', " + row[1] + ", " +  row[2] + ", " + row[3] + ", " + row[4] + ", " + row[5] + ", " + row[6] + ", " + row[7] + ", " + row[8] + ", " + row[9] + ", " + row[10] + ", " + row[11] + ");\n");
    GLOBALS.datafile.write("\n");

    # cleanup
    ifile.close()


def process_modes():
    global GLOBALS;

    # open file
    ifile = open(GLOBALS.args.mode, 'rU')
    csvreader = csv.reader(ifile, quotechar='"', delimiter=',')
    row = csvreader.next()  # ignore header

    # init
    pkey = 0;
    mtckey = 0

    # process data
    for row in csvreader:
        pkey += 1;  # increment primary key
        row[1] = row[1].replace("'", "''");  # escape single quotes for proper SQL
        GLOBALS.datafile.write("INSERT INTO modes (id, name, categories, questions, timer, misses, browsable, hints, links) values  (" +
                            str(pkey) + ", '" + str(row[0]) + "', " + row[1] + ", " +  row[2] + ", " + row[3] +
                            ", " + row[4] + ", " + row[5] + ", " +  row[6] + ", " + str(row[7]) +");\n");
        for category in GLOBALS.themecats:
            if (category['name'] == str(row[0])):                            
                GLOBALS.sqlmodes.append("INSERT INTO modes_to_categories (id, mode, category) values (" + str(mtckey) + ", " + str(pkey) +  ", " + str(category['category']) + ");");
                mtckey += 1; #increment primary key for modes_to_categories                    
    GLOBALS.datafile.write("\n");
    
    
    for sql in GLOBALS.sqlmodes: GLOBALS.datafile.write(sql + "\n");
    GLOBALS.datafile.write("\n");

    # cleanup
    ifile.close()


def process_properties():
    global GLOBALS;

    # open file
    ifile = open(GLOBALS.args.prop, 'rU')
    csvreader = csv.reader(ifile, quotechar='"', delimiter=',')
    row = csvreader.next()  # ignore header

    # init
    pkey = 0;

    # process data
    for row in csvreader:
        if (" " in row[0]):
            print "Warning: invalid property '" + row[0] + "'";
            continue;
        pkey += 1;  # increment primary key
        row[1] = row[1].replace("'", "''");  # escape single quotes for proper SQL
        GLOBALS.props[row[0]] = {"key":pkey, "property":row[0], "question":row[1], "type":row[2], "unit":row[3], "filter":row[4]};
        GLOBALS.datafile.write("INSERT INTO properties (id, name, question, type, unit, filter) values (" + str(pkey) + ", '" + row[0] + "', '" + row[1] + "', " +  row[2] + ", " + row[3] +  ", " + row[4] +");\n");
    GLOBALS.datafile.write("\n");

    # cleanup
    ifile.close()


def process_mapping():
    global GLOBALS;

    # open file
    ifile = open(GLOBALS.args.mapp, 'rU')
    csvreader = csv.reader(ifile, quotechar='"', delimiter=',')
    row = csvreader.next()  # ignore header

    # init
    tkey = 0;
    ckey = 0;
    theme = "";
    
    # process data
    for row in csvreader:
        if len(row) <= 0: continue;
        if (row[0]):
            tkey += 1;
            theme = row[0];
            GLOBALS.sqlthemes.append("INSERT INTO themes (id, name, color) values (" + str(tkey) + ", '" + row[0] + "', " + str(int(row[1], 16)) + ");");
        if (row[3]):
            ckey += 1;
            GLOBALS.sqlcats.append("INSERT INTO categories (id, displayname, name, theme) values (" + str(ckey) + ", '" + row[2] +  "', '" + row[3] + "', " + str(tkey) + ");");
            GLOBALS.sqlstats.append("INSERT INTO category_stats (id, cursor) values (" + str(ckey) + ", " + str(0) + ");");
            GLOBALS.themecats.append({"name":theme, "category":ckey});
        if (row[4]):
            if (row[4] in GLOBALS.props.keys()):
                GLOBALS.sqlctop.append("INSERT INTO categories_to_properties (category, property) values (" + str(ckey) + ", " + str(GLOBALS.props[row[4]]["key"]) + ");");
            else:
                print "Warning: cannot map unknown property '" + row[4] + "'";

    
    # output sql
    for sql in GLOBALS.sqlthemes: GLOBALS.datafile.write(sql + "\n");
    GLOBALS.datafile.write("\n");
    for sql in GLOBALS.sqlcats:   GLOBALS.datafile.write(sql + "\n");
    GLOBALS.datafile.write("\n");
    for sql in GLOBALS.sqlctop:   GLOBALS.datafile.write(sql + "\n");
    GLOBALS.datafile.write("\n");
    for sql in GLOBALS.sqlstats: GLOBALS.datafile.write(sql + "\n");
    GLOBALS.datafile.write("\n");

    # cleanup
    ifile.close()


def process_blacklist():
    global GLOBALS;

    # open file
    ifile = open(GLOBALS.args.blst, 'rU')
    csvreader = csv.reader(ifile, quotechar='"', delimiter=',')
    row = csvreader.next()  # ignore header

    # process data
    for row in csvreader:
        if (row[0]):
            row[0] = row[0].replace("'", "''");  # escape single quotes for proper SQL
            GLOBALS.datafile.write("INSERT INTO blacklist (term) values ('" + row[0] + "');\n");
    GLOBALS.datafile.write("\n");

    # cleanup
    ifile.close()


def main():
    global GLOBALS;
    data_filename  = 'data.sql';

    # handle command line args
    parser = argparse.ArgumentParser(description='Generate WikiPicsTrivia DB given schema and data.', formatter_class=argparse.RawTextHelpFormatter)
    parser.add_help = True
    parser.add_argument('-sch', '--schema',     dest='schm', help='db schema file')
    parser.add_argument('-s',   '--settings',   dest='sett', help='settings table file')
    parser.add_argument('-mo',  '--modes',      dest='mode', help='modes table file')
    parser.add_argument('-p',   '--properties', dest='prop', help='properties file')
    parser.add_argument('-m',   '--mapping',    dest='mapp', help='mapping file')
    parser.add_argument('-b',   '--blacklist',  dest='blst', help='blacklist file')
    args = parser.parse_args()
    if (not args.schm): args.schm = "schema.sql";
    if (not args.sett): args.sett = "settings.csv";
    if (not args.mode): args.mode = "modes.csv";
    if (not args.prop): args.prop = "properties.csv";
    if (not args.mapp): args.mapp = "mapping.csv";
    if (not args.blst): args.blst = "blacklist.csv";

    # check for input files
    if (not (args.schm and os.path.isfile(args.schm) and
             args.sett and os.path.isfile(args.sett) and
             args.mode and os.path.isfile(args.mode) and
             args.prop and os.path.isfile(args.prop) and
             args.mapp and os.path.isfile(args.mapp) and
             args.blst and os.path.isfile(args.blst) )):
        print "Error: missing input file(s)"
        sys.exit(2)

    # init
    GLOBALS.args = args;
    GLOBALS.datafile = open(data_filename, 'w+');

    # do it
    process_settings();
    process_properties();
    process_mapping();
    process_modes();
    process_blacklist();

    # cleanup
    GLOBALS.datafile.close();

    # copy files into final build folder
    shutil.copyfile(args.schm,     '../../WikiPicsTrivia/src/main/res/raw/' + args.schm);
    shutil.copyfile(data_filename, '../../WikiPicsTrivia/src/main/res/raw/' + data_filename);


if __name__ == "__main__":
    main()
