package work;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URISyntaxException;
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
public class Railway {
    final SimpleFeatureType TYPE;
    static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    
    public Railway() throws SchemaException, NumberFormatException, IOException{
    	TYPE = DataUtilities.createType("Location",
                "location:MultiLineString:srid=4326," + // <- the geometry attribute: Point type
                        "name:String," + // <- a String attribute
                        "number:Integer" // a number attribute
        );
    
    }
    
    public void createShpFile(String dataPath,String resultPath) throws IOException{
    	System.out.println("begin");
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
       List<String> points=new ArrayList<String>();
       for(String line=bufferedReader.readLine();line!=null;line=bufferedReader.readLine()){
    	   line=line.trim();
    	   if(line.equals("")||line.indexOf("jing")>-1){
    		   continue;
    	   }
   		   points.add(line);
       }
       MultiLineString lines= createPolyline1(points);
  		featureBuilder.add(lines);
  		featureBuilder.add(dataPath.substring(dataPath.lastIndexOf("/")+1));
  		featureBuilder.add(1);
  		SimpleFeature feature = featureBuilder.buildFeature(null);
       collection.add(feature);
       points.clear();
       
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
       } else {
           System.out.println(typeName + " does not support read/write access");
           System.exit(1);
       }
       System.out.println("done");
    }
    
    static  MultiLineString createPolyline1(List<String> pointStrs){
    	List<Coordinate> points=new ArrayList<Coordinate>();
    	for(String pointStr:pointStrs){
    		 String[] lonlats= pointStr.split(",");
    		 double lon=Double.parseDouble(lonlats[0]);
	   		 double lat=Double.parseDouble(lonlats[1]);
	   		 points.add(new Coordinate(lon,lat));
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
    	Railway test=new Railway();
    	String dataPath="F:/temdata/railway/铁路点坐标/cvs/";
    	String resultPath="F:/temdata/railway/铁路点坐标/shp2/";
    	for(int i=1;i<=11;i++){
    		test.createShpFile(dataPath+i+".csv",resultPath+i+".shp");
    	}
    }
}
