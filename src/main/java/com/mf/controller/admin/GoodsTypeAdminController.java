package com.mf.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mf.entity.GoodsType;
import com.mf.entity.Log;
import com.mf.service.GoodsService;
import com.mf.service.GoodsTypeService;
import com.mf.service.LogService;

/**
 * 后台管理角色Controller
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/admin/goodsType")
public class GoodsTypeAdminController {

	
	@Resource
	private GoodsTypeService goodsTypeService;

	@Resource
	private GoodsService goodsService;
	
	@Resource
	private LogService logService;

	/**
	 * 添加商品类别
	 * @param name
	 * @param parentId
	 * @return
	 * @throws Exception
	 */

	//保存类别逻辑
	@RequestMapping("/save")
	@RequiresPermissions(value={"商品管理","进货入库","退货出库","销售出库","客户退货","商品报损","商品报溢"},logical=Logical.OR)
	public Map<String,Object> save(String name,Integer parentId)throws Exception{
		Map<String,Object> resultMap=new HashMap<>();
		GoodsType goodsType=new GoodsType();
		goodsType.setName(name);
		goodsType.setpId(parentId); //pid为上个级别id
		goodsType.setIcon("icon-folder");
		goodsType.setState(0);
		goodsTypeService.save(goodsType);
		
		GoodsType parentGoodsType=goodsTypeService.findById(parentId);  //给选中的级别设置成1 说明有下一级了
		parentGoodsType.setState(1);
		goodsTypeService.save(parentGoodsType);
		
		logService.save(new Log(Log.ADD_ACTION,"添加商品类别信息"));
		resultMap.put("success", true);
		return resultMap;
	}
	
	/**
	 * 商品类别删除
	 * @param id
	 * @return
	 * @throws Exception
	 */

	//通过选中 的id
	@RequestMapping("/delete")
	@RequiresPermissions(value={"商品管理","进货入库","退货出库","销售出库","客户退货","商品报损","商品报溢"},logical=Logical.OR)
	public Map<String,Object> delete(Integer id)throws Exception{
		Map<String,Object> resultMap=new HashMap<>();
		if(goodsService.findByTypeId(id).size()==0){ //如果当前类没有商品
			GoodsType goodsType=goodsTypeService.findById(id);//查询选中类别
			if(goodsTypeService.findByParentId(goodsType.getpId()).size()==1){	//把其pid作为id查询同级只有1个说明就是当前要删除的
				GoodsType parentGoodsType=goodsTypeService.findById(goodsType.getpId());//找到父类
				parentGoodsType.setState(0);//把父类绝育
				goodsTypeService.save(parentGoodsType);//并保存
			}
			logService.save(new Log(Log.DELETE_ACTION,"删除商品类别信息"+goodsType));
			goodsTypeService.delete(id);//然后删除（脑洞就算不删除这个子节点遍历也查不到因为state设置成0了）
			resultMap.put("success", true);
		}else{
			resultMap.put("success", false);
			resultMap.put("errorInfo", "该类别下含有商品，不能删除！");
		}
		return resultMap;
	}

	/**
	 * 根据父节点获取所有复选框权限菜单
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/loadTreeInfo")//OR表示符合value其中之一即可
	@RequiresPermissions(value={"商品管理","进货入库","退货出库","销售出库","客户退货","当前库存查询","商品报损","商品报溢","商品采购统计","商品销售统计"},logical=Logical.OR)
	public String loadTreeInfo()throws Exception{
		logService.save(new Log(Log.SEARCH_ACTION,"查询所有商品类别信息"));
		return getAllByParentId(-1).toString(); //这个套路类似于 左边的3级表 但是比较简单因为是查询全部只有一个参数固定了
	}
	
	/**
	 * 根据父节点id和权限菜单id集合获取所有复选框菜单集合
	 * @param parentId
	 * @return
	 */
	public JsonArray getAllByParentId(Integer parentId){//第一次固定传入-1值
		JsonArray jsonArray=this.getByParentId(parentId);
		for(int i=0;i<jsonArray.size();i++){
			JsonObject jsonObject=(JsonObject) jsonArray.get(i);
			if("open".equals(jsonObject.get("state").getAsString())){
				continue;
			}else{
				jsonObject.add("children", getAllByParentId(jsonObject.get("id").getAsInt()));
			}
		}
		return jsonArray;
	}
	
	/**
	 * 根据父节点查询所有子节点
	 * @param parentId
	 * @return
	 */
	public JsonArray getByParentId(Integer parentId){
		List<GoodsType> goodsTypeList=goodsTypeService.findByParentId(parentId);
		JsonArray jsonArray=new JsonArray();
		for(GoodsType goodsType:goodsTypeList){
			JsonObject jsonObject=new JsonObject();
			jsonObject.addProperty("id", goodsType.getId()); // 节点Id
			jsonObject.addProperty("text", goodsType.getName()); // 节点名称
			if(goodsType.getState()==1){
				jsonObject.addProperty("state", "closed"); // 根节点
			}else{
				jsonObject.addProperty("state", "open"); // 叶子节点
			}
			jsonObject.addProperty("iconCls", goodsType.getIcon()); // 节点图标
			JsonObject attributeObject=new JsonObject(); // 扩展属性
			attributeObject.addProperty("state", goodsType.getState()); // 节点状态
			jsonObject.add("attributes", attributeObject); 
			jsonArray.add(jsonObject);
		}
		return jsonArray;
	}

}
