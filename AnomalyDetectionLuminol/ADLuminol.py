from luminol.anomaly_detector import AnomalyDetector
from luminol.correlator import Correlator
import json
import csv
import pandas as pd
import datetime
import glob
import natsort
from time import strptime, strftime, mktime, gmtime,localtime
import numpy as np


##################################################################################################################
#       Correlates anomalous points in the available time series to other time series                            #
##################################################################################################################
# Combines csv (per day) into csv (total)
def combineCSV(component):
    path = "cdata/" + component + "*csv"
    # path = "/Users/jessica.a.wu/Documents/Personal/2019/ESIPS/AllImplementation/AnomalyDetectionLuminol/cdata/login_190407.csv"

    files = [f for f in glob.glob(path)]
    files = set(files)
    files = natsort.natsorted(files)

    targetPath = "RawData/" + component + ".json"
    targetFile = open(targetPath, "a+")
    jsonData = {}


    # # Open each csv file
    for f in files:
        data = pd.read_csv(f, usecols=['_time','val'])
        data.dropna(subset=['val'], inplace=True)

        for index,content in data.iterrows():
            raw = content['_time']
            timestamp = strptime(raw, '%Y-%m-%dT%H:%M:%S.%f%z')
            timestampEpoch = int(mktime(timestamp))
            jsonData[timestampEpoch] = content['val']

    return jsonData
#
# # Given 2 time series, it calculates the points in time where an anomaly in ts1 correlates to ts2
# def findAnomalies(ts, thresholdVal):
#     # Conduct AD on each of each of the time series.
#     detector = AnomalyDetector(ts, score_threshold=thresholdVal, algorithm_name="exp_avg_detector")
#     anomalies = detector.get_anomalies()
#
#     # print("anomalies size = ", len(anomalies))
#
#     # For anomalous points in ts1, get time window,.
#     for a in anomalies:
#         time_period = a.get_time_window()
#
#         # Change time period to human readable format
#         start = strftime('%Y-%m-%d %H:%M:%S', localtime(a.start_timestamp))
#         end = strftime('%Y-%m-%d %H:%M:%S', localtime(a.end_timestamp))
#         time_period = (start, end)
#
#         # Extract time stamp and anomaly score
#         print("TS:", str(time_period), "|" + str(a.anomaly_score) )


def pointsOfCorrelation(ts1, ts2, thresholdVal):
    corrPoints = []

    # Conduct AD on each of each of the time series.
    # algorithm_params={'absolute_threshold_value_lower':lower,'absolute_threshold_value_upper':upper}
    # detector = AnomalyDetector(ts2, score_threshold=thresholdVal, algorithm_name="derivative_detector")
    detector = AnomalyDetector(ts2, score_threshold=thresholdVal, algorithm_name="exp_avg_detector")

    # score = detector.get_all_scores()
    anomalies = detector.get_anomalies()


    # For anomalous points in ts1, return correlated points and correlation coefficient.
    for a in anomalies:
        time_period = a.get_time_window()

        try:
            my_correlator = Correlator(ts1,ts2, time_period)

            if my_correlator.is_correlated(threshold=0.8):
                correlatorResultObj = my_correlator.get_correlation_result()

                # Change time period to human readable format
                start = strftime('%Y-%m-%d %H:%M:%S', localtime(a.start_timestamp))
                end = strftime('%Y-%m-%d %H:%M:%S', localtime(a.end_timestamp))
                time_period = (start, end)

                # Return anomalous time period, correlation coefficient and anomaly score.
                # Note: Anomaly score for absolute threshold will be diff between value and threshold.
                result = [time_period, round(correlatorResultObj.coefficient,2), round(a.anomaly_score,2)]
                corrPoints.append(result)
        except:
            continue


    return corrPoints


def callCorrelation(symptom_node, symptom_name, threshold):
    # odr1 = pointsOfCorrelation(odr1_ts, symptom_node, threshold)
    # print("\nodr - " + str(symptom_name))
    # print(odr1)

    odr2 = pointsOfCorrelation(odr2_ts, symptom_node, threshold)
    print("\nodr - " + str(symptom_name))
    print(odr2)

    odr3 = pointsOfCorrelation(odr3_ts, symptom_node, threshold)
    print("\nodr_es - " + str(symptom_name))
    print(odr3)

    odr4 = pointsOfCorrelation(odr4_ts, symptom_node, threshold)
    print("\nodr_es - " + str(symptom_name))
    print(odr4)

    db = pointsOfCorrelation(db_ts, symptom_node, threshold)
    print("\ndb - " + str(symptom_name))
    print(db)


    login = pointsOfCorrelation(login_ts, symptom_node, threshold)
    print("\nlogin - " + str(symptom_name))
    print(login)

    solr = pointsOfCorrelation(solr_ts, symptom_node,  threshold)
    print("\nsolr - " + str(symptom_name))
    print(solr)
    #
    wcs = pointsOfCorrelation(wcs_ts, symptom_node, threshold)
    print("\nwcs - " + str(symptom_name))
    print(wcs)


# def callAn():
#     print("\nName:es_rt")
#     es = findAnomalies(es_ts, 4)
#     print("\n-----------------------------------------------------------------------------")
#
#     print("\nName:odr_rt")
#     odr1 = findAnomalies(odr1_ts, 4)
#     print("\n-----------------------------------------------------------------------------")
#
#
#
#
#     print("\nName:odr_qt")
#     odr2_es = findAnomalies(odr2_ts, 4)
#     print("\n-----------------------------------------------------------------------------")
#
#     print("\nName:odr_qt")
#     odr3_es = findAnomalies(odr3_ts, 4)
#     print("\n-----------------------------------------------------------------------------")
#
#
#     print("\nName:odr_qt")
#     odr4_es = findAnomalies(odr4_ts, 4)
#     print("\n-----------------------------------------------------------------------------")
#
#
#     print("\nName:db_rt")
#     db_es = findAnomalies(db_ts, 4)
#     print("\n-----------------------------------------------------------------------------")
#
#
#     print("\nName:login_rt: ")
#     login_es = findAnomalies(login_ts, 0.5)
#     print("\n-----------------------------------------------------------------------------")
#
#     #
#     print("\nName:solr_rt")
#     solr_es = findAnomalies(solr_ts,  1)
#     print("\n-----------------------------------------------------------------------------")
#
#
#     print("\nName:wcs_rt")
#     wcs_es = findAnomalies(wcs_ts, 3)
#     print("\n-----------------------------------------------------------------------------")

# Combine all csv files into a  dictionary
es_ts = combineCSV("es")
db_ts = combineCSV("db")
login_ts = combineCSV("login")
# odr1_ts = combineCSV("odr1" )
odr2_ts = combineCSV("odr2")
odr3_ts = combineCSV("odr3")
odr4_ts = combineCSV("odr4")
solr_ts = combineCSV("solr")
wcs_ts = combineCSV("wcs")

# findAnomalies(db_ts, 0.5)

# For each anomaly, calcualate at the correlated points in time series, and their correlation score.
# CASE A:
# callCorrelation(es_ts, "es", 4);
# callCorrelation(solr_ts, "solr", 1);
# callCorrelation(db_ts, "db", 4);
# callCorrelation(login_ts, "login", 0.5);
# callCorrelation(wcs_ts,"wcs", 4);
callCorrelation(odr2_ts, "odr", 4);




# callAn()
