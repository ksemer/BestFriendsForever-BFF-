from snap import *
import sys
import random

if (len(sys.argv) != 9):
	print("Argument required: # of snapshots, # of generic graph, # of dense graph1, # of dense graph2, dense graph1 prob, dense graph2 prob, # snaps of dense graph2, # synth datasets to be extracted")
	exit()

sizeOfSnaps = (int) (sys.argv[1])
sizeOfGenericG = (int) (sys.argv[2])
sizeOfDenseG1 = (int) (sys.argv[3])
sizeOfDenseG2 = (int) (sys.argv[4])
denseG1Prob = (float) (sys.argv[5])
denseG2Prob = (float) (sys.argv[6])
snapsOfG2 = (int) (sys.argv[7])
sizeOfExtractedData = (int) (sys.argv[8])

for i in range(0, sizeOfExtractedData):

	# create a synthetic graph using forest fire model with a planted dense graph
	for snapG2 in range(2, snapsOfG2 + 1, 2):
	    output = "synthetic_pr:" + str(denseG2Prob) + "_s:" + str(snapG2) + "_it_" + str(i)
	    snaps = set()

	    # select snapG2 distinct values
	    while (len(snaps) < snapG2): 
		snaps.add(random.randint(0, sizeOfSnaps - 1))

	    with open(output, 'w') as f:
		# for each snapshot
		for snap in range(0, sizeOfSnaps):
		    # generate a graph using forest fire model
		    G = GenForestFire(sizeOfGenericG, 0.35, 0.35)

		    	# convert to undirected graph
		    G = ConvertGraph(PUNGraph, G)

		    	# create dense G1
		    for src in range(sizeOfGenericG - sizeOfDenseG1, sizeOfGenericG):
		        for trg in range(src + 1, sizeOfGenericG):
						if random.random() <= denseG1Prob:
							G.AddEdge(src, trg)
		    
		    # create dense g2 in specific snapshots
		    if snap in snaps:
		        # for each dense node
		        for src in range(sizeOfGenericG - sizeOfDenseG1 - sizeOfDenseG2, sizeOfGenericG - sizeOfDenseG1):
		            for trg in range(src + 1, sizeOfGenericG - sizeOfDenseG1):
		        	    if random.random() <= denseG2Prob:
		        		    G.AddEdge(src, trg)
		
		    # store the graph into disk
		    for EI in G.Edges():
		        f.write(str(EI.GetSrcNId()) + "\t" + str(EI.GetDstNId()) + "\t" + str(snap) + "\n")
