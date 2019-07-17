from pyqtgraph.Qt import QtGui, QtCore
import numpy as np
import pyqtgraph as pg
import pandas as pd
import pyqtgraph.exporters
import os
import glob
import natsort

# Plots pyqt multi graph
def plot_stream(arr, window, ymin, ymax):
    counter = 0
    for date, data in arr.items():
        if counter%3 == 0:
            window.nextRow()
        p = window.addPlot(title=date)
        p.plot(np.arange(len(data)), data, pen=(0,255,0))
        p.setYRange(ymin,ymax)
        counter = counter + 1

# Returns dictionary of input component
def extract_data(component):
    files = [f for f in glob.glob("cdata/" + component + "*.csv")]
    files = natsort.natsorted(files)

    data_dictionary = {}

    for f in files:
        date = f.split("/")[1].split(".")[0]
        data_dictionary[date] = pd.read_csv(f)['val'].to_numpy()

    return data_dictionary



app = QtGui.QApplication([])

# Plot ES data stream
win1 = pg.GraphicsWindow(title="ES graphs")
win1.resize(1000,600)
win1.setWindowTitle('ES graphs')
pg.setConfigOptions(antialias=True)
es = extract_data("es")
plot_stream(es, win1, 0, 6e+07)

# Plot login data stream
win2 = pg.GraphicsWindow(title="Login graphs")
win2.resize(1000,600)
win2.setWindowTitle('login graphs')
pg.setConfigOptions(antialias=True)
login = extract_data("login")
plot_stream(login, win2, 0, 6e+07)

# Plot solr data stream
win3 = pg.GraphicsWindow(title="SOLR graphs")
win3.resize(1000,600)
win3.setWindowTitle('SOLR graphs')
pg.setConfigOptions(antialias=True)
solr = extract_data("solr")
plot_stream(solr, win3, 0, 8e+07)


# Plot wcs data stream
win4 = pg.GraphicsWindow(title="WCS graphs")
win4.resize(1000,600)
win4.setWindowTitle('WCS graphs')
pg.setConfigOptions(antialias=True)
wcs = extract_data("wcs")
plot_stream(wcs, win4, 0, 9.4077e+06)


# Plot db data stream
win5 = pg.GraphicsWindow(title="DB graphs")
win5.resize(1000,600)
win5.setWindowTitle('DB graphs')
pg.setConfigOptions(antialias=True)
db = extract_data("db")
plot_stream(db, win5, 240, 340)


# Plot odr data stream
win6 = pg.GraphicsWindow(title="ODR graphs")
win6.resize(1000,600)
win6.setWindowTitle('ODR graphs')
pg.setConfigOptions(antialias=True)
odr = extract_data("odr")
plot_stream(odr, win6, 0, 300)


## Start Qt event loop unless running in interactive mode or using pyside.
if __name__ == '__main__':
    import sys
    if (sys.flags.interactive != 1) or not hasattr(QtCore, 'PYQT_VERSION'):
        QtGui.QApplication.instance().exec_()
