import datetime
import csv
import os
import glob
import pandas as pd
import math
import natsort
##################################################################################################################
#                    Discretizes continuous raw data into "value buckets" then places                            #
#                    these values into different time buckets (= 4 hours)                                        #
##################################################################################################################

def bucket_per_source(filename, metric):
    filename = filename.split(".")[0]
    fullpath="PhatCSV/" + filename + ".csv"
    csvFile = open(fullpath, "r")
    reader = csv.reader(csvFile)

    # Open bucket files and write headers
    bucket1 = open("Buckets/"+filename+"_bucket1.csv","w+")
    bucket2 = open("Buckets/"+filename+"_bucket2.csv","w+")
    bucket3 = open("Buckets/"+filename+"_bucket3.csv","w+")
    bucket4 = open("Buckets/"+filename+"_bucket4.csv","w+")
    bucket5 = open("Buckets/"+filename+"_bucket5.csv","w+")
    bucket6 = open("Buckets/"+filename+"_bucket6.csv","w+")

    # next(reader)
    for row in reader:
        # Store the time
        dt = (str(row[0]).split("T")[1]).split(".")[0]
        time = datetime.datetime.strptime(dt, "%H:%M:%S")

        # Range is per 6 hours.
        range1Start = datetime.datetime.strptime("00:00:00", "%H:%M:%S")
        range2Start = datetime.datetime.strptime("04:00:00", "%H:%M:%S")
        range3Start = datetime.datetime.strptime("08:00:00", "%H:%M:%S")
        range4Start = datetime.datetime.strptime("12:00:00", "%H:%M:%S")
        range5Start = datetime.datetime.strptime("16:00:00", "%H:%M:%S")
        range6Start = datetime.datetime.strptime("20:00:00", "%H:%M:%S")

        # Replace empty value with "delete" TODO: Remove entire rows with delete columns.
        if row[1] is "":
            row = "delete" + "\n"
        else:
            row = discretize(row[1],metric)
            row = str(row) + "\n"

        # Write row to correct bin
        if (time >= range1Start and time < range2Start):
            bucket1.write(row)
        elif (time >= range2Start and time < range3Start):
            bucket2.write(row)
        elif(time >= range3Start and time < range4Start):
            bucket3.write(row)
        elif(time >= range4Start and time < range5Start):
            bucket4.write(row)
        elif(time >= range5Start and time < range6Start):
            bucket5.write(row)
        else:
            bucket6.write(row)

    bucket1.close()
    bucket2.close()
    bucket3.close()
    bucket4.close()
    bucket5.close()
    bucket6.close()

# Features: date,time,Temperature,Humidity,Light,CO2,HumidityRatio,Occupancy
def discretize(val, metric):
    if metric is "es":
        return math.ceil(float(val)/float(15000))*15000
    # elif metric is "ors":
    #     return math.ceil(float(val)/float(800000))*800000
    elif metric is "login":
        return math.ceil(float(val)/float(400000))*400000
    elif metric is "db":
        # print("pre: ", val, ", post:",math.ceil(float(val)/float(15))*15)
        return math.ceil(float(val)/float(10))*10
    elif metric is "odr":
        return round(float(val),1)
    else: #wsc, wscdb, solr
        return math.ceil(float(val)/float(100000))*100000


# For each time period, combine columens of each types into one parent time period bucket
def combine_source(index):
    # Retrieve files associated with given bucket index
    path = "Buckets/"
    files = [f for f in glob.glob(path + "*_bucket" + str(index) + ".csv")]
    files = set(files)
    files = natsort.natsorted(files)
    print(files)

    # Read all components within the chosen time bucket
    db = pd.read_csv(files[0])
    es = pd.read_csv(files[1])
    login = pd.read_csv(files[2])
    odr = pd.read_csv(files[3])
    # ors = pd.read_csv(files[4])
    solr = pd.read_csv(files[4])
    wcs = pd.read_csv(files[5])
    # wcsdb = pd.read_csv(files[7])

    # data = pd.concat([es,db, login, solr, wcs, ors, odr, wcsdb], axis=1, join='outer', ignore_index=False)
    # data.columns = ["es_rt__" +str(index), "db_rt__"+str(index),"login_rt__"+str(index),"solr_rt__"+str(index),"wcs_rt__"+str(index),"ors_queue__"+str(index), "odr_qt__"+str(index),"wcsdb_rt__"+str(index)]
    # data.to_csv("CompleteBuckets/Bucket"+str(index)+".csv")
    data = pd.concat([es, db, login, solr, wcs, odr], axis=1, join='outer', ignore_index=False)
    data.columns = ["es_rt__" +str(index), "db_rt__"+str(index),"login_rt__"+str(index),"solr_rt__"+str(index),"wcs_rt__"+str(index), "odr_qt__"+str(index)]
    data.to_csv("CompleteBuckets/Bucket"+str(index)+".csv")

    # data = pd.concat([es, login, solr, odr, wcsdb], axis=1, join='outer', ignore_index=False)
    # data.columns = ["es_rt__" +str(index),"login_rt__"+str(index),"solr_rt__"+str(index), "odr_qt__"+str(index),"wcsdb_rt__"+str(index)]
    # data.to_csv("CompleteBuckets/Bucket"+str(index)+".csv")


def combine_total():
    bucket1 = pd.read_csv('CompleteBuckets/Bucket1.csv')
    bucket2 = pd.read_csv('CompleteBuckets/Bucket2.csv')
    bucket3 = pd.read_csv('CompleteBuckets/Bucket3.csv')
    bucket4 = pd.read_csv('CompleteBuckets/Bucket4.csv')
    bucket5 = pd.read_csv('CompleteBuckets/Bucket5.csv')
    bucket6 = pd.read_csv('CompleteBuckets/Bucket6.csv')


    result = pd.concat([bucket1, bucket2, bucket3, bucket4,bucket5,bucket6], axis=1, join='outer', ignore_index=False)
    result.to_csv("CompleteBuckets/CompleteDataset3.csv")

def clean():
    cols = list(pd.read_csv("CompleteBuckets/CompleteDataset3.csv", nrows =1))
    # print(cols)
    CompleteDataset = pd.read_csv("CompleteBuckets/CompleteDataset3.csv", usecols = [i for i in cols if 'Unnamed: 0' not in i])
    # CompleteDataset.drop("es_host1_rt__0", axis=1)
    CompleteDataset = CompleteDataset[~CompleteDataset.es_rt__1.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.es_rt__2.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.es_rt__3.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.es_rt__4.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.es_rt__5.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.es_rt__6.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.db_rt__1.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.db_rt__2.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.db_rt__3.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.db_rt__4.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.db_rt__5.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.db_rt__6.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.login_rt__1.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.login_rt__2.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.login_rt__3.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.login_rt__4.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.login_rt__5.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.login_rt__6.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.solr_rt__1.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.solr_rt__3.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.solr_rt__4.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.solr_rt__5.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.solr_rt__6.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.wcs_rt__1.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.wcs_rt__2.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.wcs_rt__3.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.wcs_rt__4.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.wcs_rt__5.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.wcs_rt__6.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.odr_qt__1.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.odr_qt__2.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.odr_qt__3.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.odr_qt__4.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.odr_qt__5.astype(str).str.contains("delete")]
    CompleteDataset = CompleteDataset[~CompleteDataset.odr_qt__6.astype(str).str.contains("delete")]

    CompleteDataset.to_csv("CompleteBuckets/CompleteDataset-clean3.csv")


def concat(component):
    path = "cdata/"
    files = [f for f in glob.glob(path + component + "*.csv")]
    files = set(files)
    files = natsort.natsorted(files)

    csvData = ""
    # Open bucket files and write headers
    finalCSV = open("PhatCSV/"+component+"_total.csv","w+")
    for f in files:

        with open(f) as partCSV:
            next(partCSV)

            for line in partCSV:
                csvData = csvData + line

    finalCSV.write(csvData)


# Concatenate all components into their own phat CSV
concat("es")
concat("db")
concat("login")
concat("odr")
# concat("ors")
concat("solr")
# concat("wcsdb")
concat("wcs")

# For each time stream, split into 4 buckets for 4 time periods
path = "PhatCSV/"

files = [f for f in glob.glob(path + "*.csv")]
files = set(files)
files = natsort.natsorted(files)
print(files)

bucket_per_source(files[0].split("/")[-1], "db")
bucket_per_source(files[1].split("/")[-1], "es")
bucket_per_source(files[2].split("/")[-1], "login")
bucket_per_source(files[3].split("/")[-1], "odr")
bucket_per_source(files[4].split("/")[-1], "solr")
bucket_per_source(files[5].split("/")[-1], "wcs")

#
# for f in files:
#     f = f.split("/")[-1]
#
#     if "db" in f:
#         bucket_per_source(f, "db")
#     elif "es" in f:
#         bucket_per_source(f, "es")
#     elif "wcs" in f:
#         bucket_per_source(f, "wcs")
#     elif "login" in f:
#         bucket_per_source(f, "login")
#     elif "solr" in f:
#         bucket_per_source(f, "solr")
#     elif "ors" in f:
#         bucket_per_source(f, "ors")
#     elif "odr" in f:
#         bucket_per_source(f, "odr")
#     else:
#         print("no file here:(")


# For each time period, combine columns of each types into one parent time period bucket

combine_source(1)
combine_source(2)
combine_source(3)
combine_source(4)
combine_source(5)
combine_source(6)

# Combine all parent buckets into one data set
combine_total()

# Clean up headers.
clean()

# Note: Some columns will have more data than others. WIll need to manually rmeove.
# Note: Need to add "subject_id" in the first col header manually.
