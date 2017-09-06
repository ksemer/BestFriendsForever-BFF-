from snap import *
import sys
import random

if (len(sys.argv) != 6):
    print("Argument required: # of snapshots, # of generic graph, # of dense graph, dense graph prob, # of dense snapshots")
    exit()

sizeOfSnaps = (int) (sys.argv[1])
sizeOfGenericG = (int) (sys.argv[2])
sizeOfDenseG = (int) (sys.argv[3])
denseGProb = (float) (sys.argv[4])
sizeOfDenseSnaps = (int) (sys.argv[5])


for ii in range(1, 2):

    output = "cont_" + str(sizeOfDenseSnaps) + "_" + str(ii)
    snaps = set()

    while (len(snaps) < sizeOfDenseSnaps):
        start = random.randint(0, sizeOfSnaps)
        print(start)
        if start + sizeOfDenseSnaps < sizeOfSnaps:
            for j in range(start, start + sizeOfDenseSnaps):
                 snaps.add(j)
            break

    print(snaps)

    # create a synthetic graph using forest fire model with a planted dense graph
    with open(output, 'w') as f:
        # for each snapshot
        for snap in range(0, sizeOfSnaps):
            # generate a graph using forest fire model
            G = GenForestFire(sizeOfGenericG, 0.35, 0.35)

            # convert to undirected graph
            G = ConvertGraph(PUNGraph,G)

            if snap in snaps:
                # for each dense node
                for src in range(sizeOfGenericG - sizeOfDenseG, sizeOfGenericG):
                    for trg in range(src + 1, sizeOfGenericG):
                        if random.random() <= denseGProb:
                            G.AddEdge(src, trg)
            
            for EI in G.Edges():
                f.write(str(EI.GetSrcNId()) + "\t" + str(EI.GetDstNId()) + "\t" + str(snap) + "\n")
