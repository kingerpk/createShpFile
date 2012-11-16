package work;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Administrator
 *
 */
public class Test {
	
	 static double ox=112.5526184;
	  static double oy=28.149544189;
	  static double sx=0.6;
	  static double sy=4;
	  static double rd= 0;
	
    final SimpleFeatureType TYPE;
    static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    
    public Test() throws SchemaException, NumberFormatException, IOException{
    	TYPE = DataUtilities.createType("Location",
                "location:MultiLineString:srid=4326"  // <- the geometry attribute: Point type
        );
    
    }
    
    /**
     * 
     * @param c是否变形
     * @throws IOException
     */
    public void createShpFile(boolean c,String dataPath,String resultPath) throws IOException{
    	
   	 /*
        * We create a FeatureCollection into which we will put each Feature created from a record
        * in the input csv data file
        */
       SimpleFeatureCollection collection = FeatureCollections.newCollection();
       /*
        * GeometryFactory will be used to create the geometry attribute of each feature (a Point
        * object for the location)
        */
      

       SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
       
       InputStreamReader r=new InputStreamReader(new FileInputStream(dataPath), "gbk");
       
       BufferedReader bufferedReader = new BufferedReader(r);
       HashMap<String, List<Coordinate>> mLineMap=new HashMap<String, List<Coordinate>>();
       GeometryBuilder gb=new GeometryBuilder();
       for(String line=bufferedReader.readLine();line!=null;line=bufferedReader.readLine()){
    	   String[] infos=line.split(",");
    	   String name=infos[2];
    	   System.out.println(name);
			Coordinate coord=null;
			try{
				coord=conventPoint(Float.parseFloat(infos[5])/100,Float.parseFloat(infos[4])/100);
			}catch (Exception e) {
				continue;
			}
		   if(mLineMap.get(name)==null){
			   List<Coordinate> points=new ArrayList<Coordinate>();
			   points.add(coord);
			   mLineMap.put(name, points);
		   }
		   else{
			   mLineMap.get(name).add(coord);
		   }
       }
       
       List<MultiLineString> mlines=createPolyline2(mLineMap);
       System.out.println(mlines.size());
       for(MultiLineString mline:mlines){
    	   featureBuilder.add(mline);
    	   collection.add(featureBuilder.buildFeature(null));
       }
       
       File newFile = new File(resultPath);

       ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

       Map<String, Serializable> params = new HashMap<String, Serializable>();
       params.put("url", newFile.toURI().toURL());
       params.put("create spatial index", Boolean.TRUE);

       ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
       newDataStore.setStringCharset(Charset.forName("GBK"));
       newDataStore.createSchema(TYPE);
       

       /*
        * You can comment out this line if you are using the createFeatureType method (at end of
        * class file) rather than DataUtilities.createType
        */
       newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
       Transaction transaction = new DefaultTransaction("create");

       String typeName = newDataStore.getTypeNames()[0];
       SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

       if (featureSource instanceof SimpleFeatureStore) {
           SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

           featureStore.setTransaction(transaction);
           try {
               featureStore.addFeatures(collection);
               transaction.commit();

           } catch (Exception problem) {
               problem.printStackTrace();
               transaction.rollback();

           } finally {
               transaction.close();
           }
           System.exit(0); // success!
       } else {
           System.out.println(typeName + " does not support read/write access");
           System.exit(1);
       }
    }
    
    private static List<MultiLineString> createPolyline2(HashMap<String, List<Coordinate>> npoints) throws UnknownHostException, IOException {
   	
    	List<MultiLineString> mulitilines=new ArrayList<MultiLineString>();
    		
    	Iterator<String> keyIter=npoints.keySet().iterator();
    	
    	while(keyIter.hasNext()){
    		String name=keyIter.next();
    		List<Coordinate> points=npoints.get(name);
    	   	LineString[] lines=new LineString[points.size()-1];
	   	    for(int i=0;i<points.size()-1;i++){
	   		   Coordinate[] coords=new Coordinate[]{points.get(i),points.get(i+1)};
	   		   lines[i]=geometryFactory.createLineString(coords);
	   	    }
	   	    MultiLineString muliline=geometryFactory.createMultiLineString(lines);
	   	    mulitilines.add(muliline);
    	}
   	  return mulitilines;
     }
    
    @org.junit.Test
    public void t() throws NumberFormatException, SchemaException, IOException{
    	Test test=new Test();
    	String dataPaht="F:/temdata/jianzhi/提供的数据/2012-11-5.txt";
    	String resultPath="F:/temdata/jianzhi/提供的数据/2012-11-05_c.shp";
    	test.createShpFile(true,dataPaht,resultPath);
    }
    
    
    public static Coordinate conventPoint(float x,float y) throws UnknownHostException, IOException{
  	  
  	  float d=new Float(Math.toRadians(rd));
  	  float tx=new Float((x-ox)*Math.cos(d)+(y-oy)*Math.sin(d)+ox);
  	  float ty=new Float(-(x-ox)*Math.sin(d)+(y-oy)*Math.cos(d)+oy);
  	  
  	  float tw=(float)((tx-ox)*sx);
  	  float th=(float)((ty-oy)*sy);
  	  tx=(float) (ox+tw);
  	  ty=(float) (oy+th);
      Coordinate point=new Coordinate(tx, ty);
  	  return point;
  	  
    }
    
    public static Coordinate conventPointBUP(float x,float y) throws UnknownHostException, IOException{
    	  
    	  float d=new Float(Math.toRadians(rd));
    	  float tx=new Float((x-ox)*Math.cos(d)+(y-oy)*Math.sin(d)+ox);
    	  float ty=new Float(-(x-ox)*Math.sin(d)+(y-oy)*Math.cos(d)+oy);
    	  
    	  float tw=(float)((tx-ox)*sx);
    	  float th=(float)((ty-oy)*sy);
    	  tx=(float) (ox+tw);
    	  ty=(float) (oy+th);
        Coordinate point=new Coordinate(tx, ty);
    	  return point;
    	  
      }
}
