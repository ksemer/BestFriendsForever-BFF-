# BestFriendsForever-BFF-
Project for Best Friends Forever (BFF): Finding Lasting Dense Subgraphs

## Getting Started

Using BFF to locate dense subgraphs in your graph history:

### Configurations for Datasets and query intervals

Open /config/settings.properties:

  a) Set RunReal to true
  
  b) Set DataPath to your main dir
  
  c) Set OutputPath for storing the results
  
  d) Set Datasets names separated by ; --> each file name must be located in the DataPath folder
  
  e) Set for each dataset the query lifespans separated by ; -> each query lifespan must be a subset of dataset's duration
  
  #### Example is given in settings.properties files

### Configurations for BFF and O^2 BFF algorithms

- config/settings.properties contains all options for enabling BFF and O^2 BFF algorithms

- All algorithms can be run simultaneously

- A separate file for each algorithm will be stored in OutPutPath.
- Each file name will contain an identifier that indicates the aggregate density function and the algorithm that is used to compute the dense subgraphs. 

  #### For example:
    m = 1 -> BFF-MM  running FindBFFMM
    
    m = 2 -> BFF-MA running FindBFFMM
    
    m = 3 -> BFF-AM running FindBFFMM
    
    m = 4 -> BFF-AA running FindBFFAA
    
    m = 5 -> BFF-AM running FindBFFG
    
    m = 6 -> BFF-MA running FindBFFG
    
    m = 7 -> BFF-MA running FindBFFAA
    
    m = 8 -> BFF-AM running FindBFFMM
    
    m = 9 -> BFF-MA running DCS algorithm
  
### Input File Format

A file with each line representing an edge between two nodes in a specific time instance.

  - For example:

      node_id \t node_id \t time_instance
  
## Running BFF system
run /java/system/Main.java

### Licensing

BestFriendsForever-BFF- is an open source product licensed under GPLv3.
