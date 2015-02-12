*******Readme Guide to Project Classes****
-Alan Casallas
-8/22/2014

Misc Info: In the LiveGraphView.java class, you can change the RESOLUTION constant to show are more detailed live graph for a large amount of packets (I guess at a performance cost).

-There were 15 classes when I stopped working with the code. Their flow is described below, and they are labeled in asterisks:

The first page that appears when the app starts is hosted by ***IntroActivity.java***. 

The user can then click on 'Begin', where they are taken to the main page, hosted by ***DataOutputActivity.java***.

If the user then clicks on the top-most button to select a bluetooth device, they are taken to the page hosted by ***BluetoothListActivity.java***. When they choose to connect to a device, the resulting dialog window is hosted by ***ConnectingFragment.java***.

All Bluetooth data (adapter, device, socket) is saved in a static object named ***BluetoothCommand.java***.

Back in the DataOutputActivity.java page, when the user chooses to press the 'ON' button to start streaming, the ***BluetoothThread.java*** class opens a separate thread and does the streaming. It communicates with the main thread by sending it information in a class known as ***ThreadNugget.java***.

The Bluetooth bytes themseleves are stored using two classes. ***Point3D.java*** is a general purpose class for saving xyz coordinates, and ***DataPacket.java*** represents each 21-byte packet of information received via Bluetooth.

The user can also choose options for the graphs that appear at the bottom of the page. When the user clicks the 'Graph Options' button, the dialog that opens is hosted by the ***GraphOptionsFragment.java*** class. There is also a ***GraphOptions.java*** object pass between the fragment and the data output activity that contains all the graph options.

***LiveGraphView.java*** hosts the surface view that shows the live graph that displays Bluetooth data. It runs its own thread to display the graph.

All user preferences for writing data are saved in the ***WriteOptions.java*** class. When the user presses the 'Write to File' button, the dialog that opens is hosted by ***WriteFragment.java***. When the user starts writing, the class ***WriteHandlerThread.java*** performs the writing.

