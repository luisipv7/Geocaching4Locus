package geocaching.api.data;

import geocaching.api.data.type.WayPointType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;

public class WayPoint {
	private static final int VERSION = 1;
	
	private final double latitude;
	private final double longitude;
	private final Date time;
	private final String waypointGeoCode;
	private final String name;
	private final String note;
	private final WayPointType wayPointType;
	private final String iconName;
		
	
	public WayPoint(double longitude, double latitude, Date time, String waypointGeoCode, String name, String note, WayPointType wayPointType) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.time = time;
		this.waypointGeoCode = waypointGeoCode;
		this.name = name;
		this.note = note;
		this.wayPointType = wayPointType;
		this.iconName = wayPointType.getIconName();
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public Date getTime() {
		return time;
	}
	
	public String getWaypointGeoCode() {
		return waypointGeoCode;
	}
	
	public String getName() {
		return name;
	}
	
	public String getNote() {
		return note;
	}
	
	public WayPointType getWayPointType() {
		return wayPointType;
	}
	
	public String getIconName() {
		return iconName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Method m : getClass().getMethods()) {
			if (!m.getName().startsWith("get") ||
			    m.getParameterTypes().length != 0 ||  
			    void.class.equals(m.getReturnType()))
			    continue;
			
			sb.append(m.getName());
			sb.append(':');
			try {
				sb.append(m.invoke(this, new Object[0]));
			} catch (Exception e) {}
			sb.append("; ");
		}
		return sb.toString();
	}

	public PointGeocachingDataWaypoint toPointGeocachingDataWaypoint() {
		PointGeocachingDataWaypoint w = new PointGeocachingDataWaypoint();
		w.lat = latitude;
		w.lon = longitude;
		w.description = note;
		w.name = name;
		w.typeImagePath = iconName;
		w.type = wayPointType.getId();
		
		return w;
	}

	public static WayPoint load(DataInputStream dis) throws IOException {
		if (dis.readInt() != VERSION)
			throw new IOException("Wrong waypoint version.");
		
		return new WayPoint(
				dis.readDouble(),
				dis.readDouble(),
				new Date(dis.readLong()),
				dis.readUTF(), 
				dis.readUTF(), 
				dis.readUTF(),
				WayPointType.parseWayPointType(dis.readUTF())
		);
	}

	public void store(DataOutputStream dos) throws IOException {
		dos.writeInt(VERSION);
		
		dos.writeDouble(longitude);
		dos.writeDouble(latitude);
		dos.writeLong(time.getTime());
		dos.writeUTF(waypointGeoCode);
		dos.writeUTF(name);
		dos.writeUTF(note);
		dos.writeUTF(wayPointType.toString());
	}
}
