package com.aote.rs;

import java.util.Arrays;
import java.util.Map;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.SetType;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * 元数据服务
 * 
 *
 */
@Path("db")
@Scope("prototype")
@Component
public class DBService {
	static Logger log = Logger.getLogger(DBService.class);

	@Autowired
	private SessionFactory sessionFactory;

	@Path("meta")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public String getMetaOfTables(String tables) 
	{		
		return getMetas(tables).toString();
	}
	
	public JSONObject getMetas(@QueryParam("tables") String tables) {
		String[] sTables = null;
		// 不为空，是要取的表数据名称
		if (tables != null) {
			sTables = tables.split(",");
			Arrays.sort(sTables);
		}

		JSONObject result = new JSONObject();
		// 获取所有实体
		Map<String, ClassMetadata> map = sessionFactory.getAllClassMetadata();
		for (Map.Entry<String, ClassMetadata> entry : map.entrySet()) {
			try {
				String key = entry.getKey();

				// 如果key不在所需表名里面
				if (sTables != null && Arrays.binarySearch(sTables, key) == -1) {
					continue;
				}

				JSONObject attrs = new JSONObject();
				for (String name : entry.getValue().getPropertyNames()) {
					Type type = entry.getValue().getPropertyType(name);
					attrs.put(name, TypeToString(type));
				}
				// 添加id，id号没有当做属性获取
				String idName = entry.getValue().getIdentifierPropertyName();
				Type idType = entry.getValue().getIdentifierType();
				attrs.put(idName, TypeToString(idType));
				result.put(key, attrs);
			} catch (JSONException e) {
				throw new WebApplicationException(400);
			}
		}
		log.debug(result);
		return result;
	}

	private String TypeToString(Type type) {
		if (type instanceof ManyToOneType) {
			ManyToOneType t = (ManyToOneType) type;
			String entityName = t.getAssociatedEntityName();
			return entityName;
		} else if (type instanceof SetType) {
			String entityName = getCollectionEntityName((SetType) type);
			return entityName + "[]";
		} else {
			return type.getName();
		}
	}

	// 得到集合类型的关联实体类型
	private String getCollectionEntityName(SetType type) {
		SessionFactoryImplementor sf = (SessionFactoryImplementor) sessionFactory;
		String entityName = type.getAssociatedEntityName(sf);
		return entityName;
	}

}
