package io.renren.modules.test.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.renren.modules.test.entity.JMXThreadGroup;
import io.renren.modules.test.service.StressJMXService;

@Service("stressJMXService")
public class StressJMXServiceImpl implements StressJMXService {


	/**
	 * 获取jmx脚本文件中【线程组】组件的参数
	 */
	@Override
	@Transactional
	public List<JMXThreadGroup> queryJMXFile(String path) {		
				
		//根据路径读取jmx脚本文件
		File testxml = new File(path);
		Document doc = null;
		try {
			doc = new SAXReader().read(testxml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		
		//获取xml子节点
		Element jmeterTestPlan = doc.getRootElement();
		Element hashTree1 = (Element) jmeterTestPlan.element("hashTree");
		Element hashTree2 = (Element) hashTree1.element("hashTree");
		
		List<JMXThreadGroup> threadGrouplist = new ArrayList<JMXThreadGroup>();
		
		//轮询【Test Plan】下所有子节点，找出所有【线程组】组件
        for(Iterator it=hashTree2.elementIterator();it.hasNext();){      
             Element element = (Element) it.next();      
             if ("ThreadGroup".equals(element.getName())) {    
            	 
            	 JMXThreadGroup jmxThreadGroup = new JMXThreadGroup();
            	 jmxThreadGroup.setTestname(element.attributeValue("testname"));
            	 jmxThreadGroup.setEnabled(Boolean.valueOf(element.attributeValue("enabled")));
            	 
            	 //在【线程组】组件内轮询所有子节点
	           	 for(Iterator it2 = element.elementIterator();it2.hasNext();){
		           		Element threadGroupelement = (Element) it2.next(); 
		           		
		           		if ("ThreadGroup.num_threads".equals(threadGroupelement.attributeValue("name"))) {
		           			jmxThreadGroup.setNum_threads(threadGroupelement.getText());
		           			
						}else if ("ThreadGroup.ramp_time".equals(threadGroupelement.attributeValue("name"))) {
		           			jmxThreadGroup.setRamp_time(threadGroupelement.getText());    
		           			
						}else if ("ThreadGroup.scheduler".equals(threadGroupelement.attributeValue("name"))) {
		           			jmxThreadGroup.setScheduler(Boolean.valueOf(threadGroupelement.getText()));
		           			
						}else if ("ThreadGroup.duration".equals(threadGroupelement.attributeValue("name"))) {
		           			jmxThreadGroup.setDuration(threadGroupelement.getText());
		           			
						} 		           		    		           			           		
		         }
	           	 
	           	threadGrouplist.add(jmxThreadGroup);         	 
			}
         } 
        return threadGrouplist;
				
	}

	
	/**
	 * 修改【线程组】组件内的【线程数量、多少秒内启动全部线程、是否开启自动停止、运行多少时间（秒）】 
	 */
	@Override
	@Transactional
	public void update(String path, List<JMXThreadGroup> threadGroupList) {
		
		//根据路径读取jmx脚本文件
		File testxml = new File(path);
		Document doc = null;
		try {
			doc = new SAXReader().read(testxml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		
		//获取xml子节点
		Element jmeterTestPlan = doc.getRootElement();
		Element hashTree1 = (Element) jmeterTestPlan.element("hashTree");
		Element hashTree2 = (Element) hashTree1.element("hashTree");
				
		JMXThreadGroup jmxThreadGroup = new JMXThreadGroup();
		
		
		for (int i = 0; i < threadGroupList.size(); i++) {
		
			jmxThreadGroup = threadGroupList.get(i);
			
			//轮询【Test Plan】下所有子节点，找出所有【线程组】组件
	        for(Iterator it=hashTree2.elementIterator();it.hasNext();){
	             Element element = (Element) it.next();      
	             if (jmxThreadGroup.getTestname().equals(element.attributeValue("testname"))) {    
	            	 
	            	 element.setAttributeValue("testname", String.valueOf(jmxThreadGroup.getTestname()));
	            	 element.setAttributeValue("enabled", String.valueOf(jmxThreadGroup.getEnabled()));
	            	 
	            	 //在【线程组】组件内轮询所有子节点
		           	 for(Iterator it2 = element.elementIterator();it2.hasNext();){
			           		Element threadGroupelement = (Element) it2.next(); 
			           		
			           		if ("ThreadGroup.num_threads".equals(threadGroupelement.attributeValue("name"))) {
			           			threadGroupelement.setText(jmxThreadGroup.getNum_threads());
			           			
							}else if ("ThreadGroup.ramp_time".equals(threadGroupelement.attributeValue("name"))) {
								threadGroupelement.setText(jmxThreadGroup.getRamp_time());
			           			
							}else if ("ThreadGroup.scheduler".equals(threadGroupelement.attributeValue("name"))) {
								threadGroupelement.setText(String.valueOf(jmxThreadGroup.getScheduler()));
			           			
							}else if ("ThreadGroup.duration".equals(threadGroupelement.attributeValue("name"))) {
								threadGroupelement.setText(jmxThreadGroup.getDuration());			           			
							} 		           		    		           			           		
			         }		           	         	 
				}
	         } 
		}
		
		//重新写入jmx脚本文件
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("UTF-8");
		
		try {
			XMLWriter writer = new XMLWriter(new FileOutputStream(path),format);
			writer.write(doc);
			writer.close();			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();			
		} catch (IOException e) {
			e.printStackTrace();
		}        			
	}

    
    
}
