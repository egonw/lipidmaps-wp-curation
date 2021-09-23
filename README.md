# WikiPathways Curation Template

Template repository to set up pathway curation for the subset of WikiPathways, as
specified in the `pathways.txt` file.

## Step 0. Requirements

You need the following programs and experience with a `Makefile`:

* curl
* git
* zip
* java
* javac
* bash

## Step 1: Update the GPML files

First, make sure the `pathways.txt` files is updated to list the pathways you want
to curate.

Then, do:

```
make fetch
```

## Step 3: Make sure to have the BridgeDb ID mapping databases

Create a folder `/tmp/OPSBRIDGEDB` where you create a `config.properties` file.
To download the BridgeDb identifier mapping files, download them from
[here](https://bridgedb.github.io/data/gene_database/)
and save them in the `/path/to/where/the/bridge/files/are` folder, mathching what
you entered in the `config.properties` file above with the `bridgefiles=` parameter.
You also want to download the identifier mapping database for coronavirus
genes and proteins.

## Step 4: Create the RDF (Turtle)

```
make
```

Make sure that the GPML and the `.rev` files are under version control to track
the history of the pathway.

### Step 5: Run the validation tests

```
make check
```

### Step 6: (optional) Publish everything online

For this, enable GitHub Pages and add the files in `reports/` to the revision
control, including the `index.md` in the root folder.
