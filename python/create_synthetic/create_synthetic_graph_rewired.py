from snap import *
import sys
import random

if (len(sys.argv) != 5):
	print("Argument required: # of snapshots, # of generic graph, # of dense graph, dense graph prob")
	exit()

sizeOfSnaps = (int) (sys.argv[1])
sizeOfGenericG = (int) (sys.argv[2])
sizeOfDenseG = (int) (sys.argv[3])
denseGProb = (float) (sys.argv[4])

output = "synthetic_pr:" + str(denseGProb) + "_ds:" + str(sizeOfDenseG)

# create a synthetic graph using forest fire model with a planted dense graph
with open(output, 'w') as f:
    # for each snapshot
    for snap in range(0, sizeOfSnaps):
        # generate a graph using forest fire model
	G = GenForestFire(sizeOfGenericG, 0.35, 0.35)

	# convert to undirected graph
	G = ConvertGraph(PUNGraph,G)

	print("AA: " + str(G.GetEdges()/G.GetNodes()) + "\n")
	# for each dense node
	for src in range(sizeOfGenericG - sizeOfDenseG, sizeOfGenericG):
	    for trg in range(src + 1, sizeOfGenericG):
		if random.random() <= denseGProb:
		    G.AddEdge(src, trg)

		    rem = -1
		    
		    for trg in G.GetNI(src).GetOutEdges():
			if trg < (sizeOfGenericG - sizeOfDenseG):
                            rem = trg
			    break
			
		    if rem != -1:
		    	G.DelEdge(src, rem)

	print("AA: " + str(G.GetEdges()/G.GetNodes()) + "\n")

        for EI in G.Edges():
            f.write(str(EI.GetSrcNId()) + "\t" + str(EI.GetDstNId()) + "\t" + str(snap) + "\n")
