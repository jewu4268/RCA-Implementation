import pandas as pd
import glob



def CombineByCase(filepath, outputname):
# Retrieve files associated with given bucket index
    path = filepath
    files = [f for f in glob.glob(path + "*.csv")]
    print(files)

    completeCSV = []

    for filename in files:
        print(filename)
        df = pd.read_csv(filename)
        df['filename'] = filename
        print(filename)
        node = filename.split("DBCI_")[1].split(".")[0]
        df['symptom']=node
        completeCSV.append(df)

    frame = pd.concat(completeCSV, axis = 0, ignore_index=True)

    frame.to_csv("All/" + outputname + ".csv")



CombineByCase("Mori/CaseA/", "MoriCaseA")
CombineByCase("Mori/CaseB/", "MoriCaseB")
CombineByCase("Proposed/CaseA/", "ProposedCaseA")
CombineByCase("Proposed/CaseB/", "ProposedCaseB")
CombineByCase("Wang/CaseA/", "WangCaseA")
CombineByCase("Wang/CaseB/", "WangCaseB")
