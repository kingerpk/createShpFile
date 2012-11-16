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
import java.util.List;
import java.util.Map;

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
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * @author Administrator
 *
 */
public class Ys {
	
	  static double ox=113.264988643;
	  static double oy=29.465193982;
	  static double sx=0.1;
	  static double sy=1;
	  static double rd= 53.1112;
	
    final SimpleFeatureType TYPE;
    static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    
    public Ys() throws SchemaException, NumberFormatException, IOException{
    	TYPE = DataUtilities.createType("Location",
                "location:MultiLineString:srid=4326," + // <- the geometry attribute: Point type
                        "name:String," + // <- a String attribute
                        "number:Integer" // a number attribute
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
       for(String line=bufferedReader.readLine();line!=null;line=bufferedReader.readLine()){
       	if(line.trim().equals(""))line=bufferedReader.readLine();
       	String lineAttr=line;
       	System.out.println(lineAttr);
       	line=bufferedReader.readLine();
   		line=line.substring(line.indexOf(",")+1);
   		MultiLineString lines= createPolyline2(line,c);
   		featureBuilder.add(lines);
   		featureBuilder.add(lineAttr);
   		featureBuilder.add(1);
   		SimpleFeature feature = featureBuilder.buildFeature(null);
        collection.add(feature);
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
    
    private static MultiLineString createPolyline2(String dataLine,boolean c) throws UnknownHostException, IOException {
   	 String[] lonlats= dataLine.split(",");
   	 List<Coordinate> points=new ArrayList<Coordinate>();
   	 for(int i=0;i<=lonlats.length-2;i+=2){
   		 double lon=Double.parseDouble(lonlats[i]);
   		 double lat=Double.parseDouble(lonlats[i+1]);
   		 if(c){
   			 points.add(conventPoint((float)lon,(float)lat));
   		 }else{
   			points.add(new Coordinate(lon,lat));
   		 }
   	 }
   	 if(points.size()<2){
   		 return null;
   	 }
   	LineString[] lines=new LineString[points.size()-1];
   	   for(int i=0;i<points.size()-1;i++){
   		   Coordinate[] coords=new Coordinate[]{points.get(i),points.get(i+1)};
   		   lines[i]=geometryFactory.createLineString(coords);
   	   }
   	   
   	  MultiLineString muliline=geometryFactory.createMultiLineString(lines);
   	  return muliline;
     }
    
    @org.junit.Test
    public void t() throws NumberFormatException, SchemaException, IOException{
    	Ys test=new Ys();
    	String dataPaht="f:/temdata/jianzhi/yx.txt";
    	String resultPath="f:/temdata/jianzhi/gtPoint_c.shp";
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
