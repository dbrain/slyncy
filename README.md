# Slyncy
Slyncy is an example application (read: slop) using CouchDb on Android. It was bashed out for a blog entry [here](http://wp.me/p1IYp8-nR).
This is based on [AndroidGrocerySync](https://github.com/couchbaselabs/AndroidGrocerySync).

## Installation
This is a standard Android Java project, just check it out and run via Eclipse.

## Requirements
- A recent version of Android in your emulator or phone
- A CouchDb instance running somewhere that the phone can see (optional)

## Features
- Add/edit documents with random fields to local CouchDb
- Delete documents from local CouchDb
- Push and pull replication to a remote CouchDb of your choice
- Changes driven list of items, so can see things update from backend in near real time

## Usage
- Add new item to create a document
- Put in some random fields
- Done to save
- Menu -> Settings to specify remote CouchDb
- Menu -> Start Replication to replicate this document to remote
- Play around editing / adding docs on both side and watch the replication happen
- ???
- Profit
