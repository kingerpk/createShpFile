/**
 * 
 */
package work;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author lin
 *
 */
public class JianzhiCreataArgs {
	
	
	@Test
	public void createArgs() throws Exception{
		String ytpPath="F:/temdata/jianzhi/提供的数据/5_yt.shp";//原图点
		String yspPath="F:/temdata/jianzhi/提供的数据/5_yx.shp";//映射点
		String ytlPath="F:/temdata/jianzhi/提供的数据/5_yt_l.shp";//原图宽
		String yslPath="F:/temdata/jianzhi/提供的数据/5_ys_l.shp";//映射宽
		String yt_cpPath="F:/temdata/jianzhi/提供的数据/5_cp.shp";//原图域
		SimpleFeatureSource ytpSfs=getFeaturesSource(ytpPath);
		SimpleFeatureSource yspSfs=getFeaturesSource(yspPath);
		SimpleFeatureSource ytlSfs=getFeaturesSource(ytlPath);
		SimpleFeatureSource yslSfs=getFeaturesSource(yslPath);
		SimpleFeatureSource gt_cpSfs=getFeaturesSource(yt_cpPath);
		
		SimpleFeatureCollection yt_cpSfc=gt_cpSfs.getFeatures();
		SimpleFeatureIterator sfi=yt_cpSfc.features();
		List<String> areaIDs=new ArrayList<String>();
		while(sfi.hasNext()){
			SimpleFeature sf=sfi.next();
			Object value=sf.getAttribute("Id");
			
			if(value==null||value.toString().trim().equals("")){
				throw new Exception("gt_cp："+sf.getID()+"有误");
			}
			areaIDs.add(value.toString());
		}
		
		for(String aId:areaIDs){
			StringBuilder strBuilder=new StringBuilder("a"+aId+":{\n");
			List<SimpleFeature> ytpFs=getFeaturesYTP(ytpSfs, aId);
			List<SimpleFeature> yspFs=getFeaturesYSP(yspSfs, aId);
			List<SimpleFeature> ytlFs=getFeaturesl(ytlSfs, aId);
			List<SimpleFeature> yslFs=getFeaturesl(yslSfs, aId);
			if(ytpFs==null||yspFs==null||ytlFs==null||yslFs==null){
				System.out.println(aId+" is nodata");
				continue;
			}
			for(int i=0;i<4;i++){
				Point ytp=(Point) ytpFs.get(i).getDefaultGeometry();
				Point ysp=(Point) yspFs.get(i).getDefaultGeometry();
				strBuilder.append("\tp"+(i+1)+":{yt:["+ytp.getX()+","+ytp.getY()+"],ys:["+ysp.getX()+","+ysp.getY()+"]},\n");
			}
			Point opp=(Point) ytpFs.get(4).getDefaultGeometry();
			strBuilder.append("\top:{x:"+opp.getX()+",y:"+opp.getY()+"},\n");
			MultiLineString ytlineW=(MultiLineString)ytlFs.get(0).getDefaultGeometry();
			MultiLineString yslineW=(MultiLineString)yslFs.get(0).getDefaultGeometry();
			MultiLineString ytlineH=(MultiLineString)ytlFs.get(1).getDefaultGeometry();
			MultiLineString yslineH=(MultiLineString)yslFs.get(1).getDefaultGeometry();
			DecimalFormat df2  = new DecimalFormat("0.00000000"); 
			strBuilder.append("\th:{yt:"+df2.format(ytlineH.getLength())+",ys:"+df2.format(yslineH.getLength())+"},\n");
			strBuilder.append("\tw:{yt:"+df2.format(ytlineW.getLength())+",ys:"+df2.format(yslineW.getLength())+"}\n");
			strBuilder.append("},");
			System.out.println(strBuilder.toString());
		}
		
	}
	
	public SimpleFeatureSource getFeaturesSource(String path) throws Exception{
		Map<String, URL> params=new HashMap<String, URL>();
		params.put("url", new File(path).toURL());
		DataStore datastore=DataStoreFinder.getDataStore(params);
		String name=path.substring(path.lastIndexOf("/")+1,path.lastIndexOf("."));
		SimpleFeatureSource sf= datastore.getFeatureSource(name);
		return sf;
	}
	
	public List<SimpleFeature> getFeaturesYTP(SimpleFeatureSource sfs,String areaId) throws Exception{
		SimpleFeatureCollection sfc=sfs.getFeatures(CQL.toFilter("areaId="+areaId));
		if(sfc.size()<5||sfc.size()>5){
			System.out.println(areaId+" 原图点异常");
			return null;
		}
		SimpleFeature[] sfsarray=(SimpleFeature[]) sfc.toArray();
		List<SimpleFeature> sfList=new ArrayList<SimpleFeature>();
		for(int i=0;i<5;i++){
			SimpleFeature sf=sfsarray[i];
			sfList.add(sf);
		}
		Collections.sort(sfList, new featureCompater());
		return sfList;
	}
	
	public List<SimpleFeature> getFeaturesYSP(SimpleFeatureSource sfs,String areaId) throws Exception{
		SimpleFeatureCollection sfc=sfs.getFeatures(CQL.toFilter("areaId="+areaId));
		if(sfc.size()<4||sfc.size()>4){
			System.out.println(areaId+" 映射点异常");
			return null;
		}
		SimpleFeature[] sfsarray=(SimpleFeature[]) sfc.toArray();
		List<SimpleFeature> sfList=new ArrayList<SimpleFeature>();
		for(int i=0;i<4;i++){
			SimpleFeature sf=sfsarray[i];
			sfList.add(sf);
		}
		Collections.sort(sfList, new featureCompater());
		return sfList;
	}
	
	public List<SimpleFeature> getFeaturesl(SimpleFeatureSource sfs,String areaId) throws Exception{
		SimpleFeatureCollection sfc=sfs.getFeatures(CQL.toFilter("areaId="+areaId));
		if(sfc.size()<2){
			
			return null;
			//throw new Exception(areaId+"有误");
		}
		SimpleFeature[] sfsarray=(SimpleFeature[]) sfc.toArray();
		List<SimpleFeature> sfList=new ArrayList<SimpleFeature>();
		for(int i=0;i<2;i++){
			SimpleFeature sf=sfsarray[i];
			sfList.add(sf);
		}
		Collections.sort(sfList, new featureCompater());
		return sfList;
	}
	
	class featureCompater implements Comparator<SimpleFeature>{

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(SimpleFeature o1, SimpleFeature o2) {
			int id1= Integer.parseInt(o1.getAttribute("Id").toString());
			int id2= Integer.parseInt(o2.getAttribute("Id").toString());
			if(id1>id2){
				return 1;
			}
			else if(id1<id2){
				return -1;
			}
			return 0;
		}
		
		
	}
	
}
