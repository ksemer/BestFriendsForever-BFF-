from snap import *
import sys
import random

if (len(sys.argv) != 7):
    print("Argument required: # of snapshots, # of generic graph, # of dense graph, dense graph prob, # of dense snapshots, # synth datasets to be extracted")
    exit()

sizeOfSnaps = (int) (sys.argv[1])
sizeOfGenericG = (int) (sys.argv[2])
sizeOfDenseG = (int) (sys.argv[3])
denseGProb = (float) (sys.argv[4])
sizeOfDenseSnaps = (int) (sys.argv[5])
sizeOfExtractedData = (int) (sys.argv[6])

for i in range(0, sizeOfExtractedData):
	print("Graph: " + str(i) + " creation started")
	output = "syntho2_k:" + str(sizeOfDenseSnaps) + "_i:" + str(i)
	snaps = set()
	denseNodes = set()

	while len(denseNodes) < sizeOfDenseG:
		denseNodes.add(random.randint(0, sizeOfGenericG - 1))

	with open(output + "_nodes", 'w') as f1:
		for n in denseNodes:
			f1.write(str(n) + "\n")

	while (len(snaps) < sizeOfDenseSnaps):
	    snaps.add(random.randint(0, sizeOfSnaps - 1))

	# create a synthetic graph using forest fire model with a planted dense graph
	with open(output, 'w') as f:
	    # for each snapshot
	    for snap in range(0, sizeOfSnaps):
		
			# generate a graph using forest fire model
			G = GenForestFire(sizeOfGenericG, 0.35, 0.35)

			if snap in snaps:
		    	# for each dense node
				for src in denseNodes:
					for trg in denseNodes:
						if src != trg and random.random() <= denseGProb:
							G.AddEdge(src, trg)

			# convert to undirected graph
			G = ConvertGraph(PUNGraph,G)

			print("Average Degree in " + str(snap) + " : " + str(G.GetEdges()/G.GetNodes()) + "\n")
		
			for EI in G.Edges():
				f.write(str(EI.GetSrcNId()) + "\t" + str(EI.GetDstNId()) + "\t" + str(snap) + "\n")

	print(snaps)