from snap import *
import sys
import random

if (len(sys.argv) != 6):
	print("Argument required: # of snapshots, # of generic graph, # of dense graph, dense graph prob, # of graphs")
	exit()

sizeOfSnaps = (int) (sys.argv[1])
sizeOfGenericG = (int) (sys.argv[2])
sizeOfDenseG = (int) (sys.argv[3])
denseGProb = (float) (sys.argv[4])
numberOfGraphs = (int) (sys.argv[5])

for i in range (0, numberOfGraphs):
	print("Graph: " + str(i) + " creation started")
	output = "synth_pr:" + str(denseGProb) + "_i:" + str(i)

	with open(output, 'w') as f:

		denseNodes = set()

		while len(denseNodes) < sizeOfDenseG:
			denseNodes.add(random.randint(0, sizeOfGenericG - 1))

		# for each snapshot
		for snap in range(0, sizeOfSnaps):
			G = GenForestFire(sizeOfGenericG, 0.35, 0.35)

			for src in denseNodes:
				for trg in denseNodes:
					if src != trg and random.random() <= denseGProb:
						G.AddEdge(src, trg)
			
			# convert to undirected graph
			G = ConvertGraph(PUNGraph,G)

			for EI in G.Edges():
				f.write(str(EI.GetSrcNId()) + "\t" + str(EI.GetDstNId()) + "\t" + str(snap) + "\n")

        with open(output + "_nodes", 'w') as f1:
        	for n in denseNodes:
        		f1.write(str(n) + "\n")