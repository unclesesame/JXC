package com.mf.service.impl;


import java.util.List;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mf.entity.Goods;
import com.mf.entity.PurchaseList;
import com.mf.entity.PurchaseListGoods;
import com.mf.repository.GoodsRepository;
import com.mf.repository.GoodsTypeRepository;
import com.mf.repository.PurchaseListGoodsRepository;
import com.mf.repository.PurchaseListRepository;
import com.mf.service.PurchaseListService;
import com.mf.util.MathUtil;
import com.mf.util.StringUtil;

/**
 * 进货单Service实现类
 * @author Administrator
 *
 */
@Service("purchaseListService")
public class PurchaseListServiceImpl implements PurchaseListService{

	@Resource
	private PurchaseListRepository purchaseListRepository;
	
	@Resource
	private GoodsTypeRepository goodsTypeRepository;
	
	@Resource
	private GoodsRepository goodsRepository;
	
	@Resource
	private PurchaseListGoodsRepository purchaseListGoodsRepository;

	//最大单号
	@Override
	public String getTodayMaxPurchaseNumber() {
		return purchaseListRepository.getTodayMaxPurchaseNumber();
	}

	@Transactional
	public void save(PurchaseList purchaseList, List<PurchaseListGoods> purchaseListGoodsList) {//添加商品也许有多个 so  list类型
		for(PurchaseListGoods purchaseListGoods:purchaseListGoodsList){
			purchaseListGoods.setType(goodsTypeRepository.findOne(purchaseListGoods.getTypeId())); // 设置类别用当前的typeid=id去goodstype查
			purchaseListGoods.setPurchaseList(purchaseList); // set两个pojo
			purchaseListGoodsRepository.save(purchaseListGoods);//被封装了，保存但是sql里没有这两个字段
			// 修改商品库存 成本均价 以及上次进价
			Goods goods=goodsRepository.findOne(purchaseListGoods.getGoodsId()); //找到对应的goods
										//之前采购价格 * 采购数量  + 前端返回这次的单价*数量    最后除以  总共的数量（前端+之前的）  最后是进购商品的平均值
			float svePurchasePrice=(goods.getPurchasingPrice()*goods.getInventoryQuantity()+purchaseListGoods.getPrice()*purchaseListGoods.getNum())/(goods.getInventoryQuantity()+purchaseListGoods.getNum());
			goods.setPurchasingPrice(MathUtil.format2Bit(svePurchasePrice)); //把平均数设置给goods表单
			goods.setInventoryQuantity(goods.getInventoryQuantity()+purchaseListGoods.getNum());//购进总数设置给goods
			goods.setLastPurchasingPrice(purchaseListGoods.getPrice());//最后购进的单价
			goods.setState(2);//有进货 约定值
			goodsRepository.save(goods); //保存goods
		}
		purchaseListRepository.save(purchaseList); // 保存进货单
	}

	@Override
	public List<PurchaseList> list(PurchaseList purchaseList, Direction direction, String... properties) {
		return purchaseListRepository.findAll(new Specification<PurchaseList>() {
			
			@Override
			public Predicate toPredicate(Root<PurchaseList> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				Predicate predicate=cb.conjunction();
				if(purchaseList!=null){
					if(StringUtil.isNotEmpty(purchaseList.getPurchaseNumber())){
						predicate.getExpressions().add(cb.like(root.get("purchaseNumber"), "%"+purchaseList.getPurchaseNumber().trim()+"%"));
					}
					if(purchaseList.getSupplier()!=null && purchaseList.getSupplier().getId()!=null){
						predicate.getExpressions().add(cb.equal(root.get("supplier").get("id"), purchaseList.getSupplier().getId()));
					}
					if(purchaseList.getState()!=null){
						predicate.getExpressions().add(cb.equal(root.get("state"), purchaseList.getState()));
					}
					if(purchaseList.getbPurchaseDate()!=null){
						predicate.getExpressions().add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), purchaseList.getbPurchaseDate()));
					}
					if(purchaseList.getePurchaseDate()!=null){
						predicate.getExpressions().add(cb.lessThanOrEqualTo(root.get("purchaseDate"), purchaseList.getePurchaseDate()));
					}
				}
				return predicate;
			}
		},new Sort(direction, properties));
	}

	@Override
	public PurchaseList findById(Integer id) {
		return purchaseListRepository.findOne(id);
	}

	@Transactional
	public void delete(Integer id) {
		purchaseListGoodsRepository.deleteByPurchaseListId(id);
		purchaseListRepository.delete(id);//陪封装了 删除当前订单表
	}

	@Override
	public void update(PurchaseList purchaseList) {
		purchaseListRepository.save(purchaseList);
	}

	

}
