package com.mf.realm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.mf.entity.Menu;
import com.mf.entity.Role;
import com.mf.entity.User;
import com.mf.repository.MenuRepository;
import com.mf.repository.RoleRepository;
import com.mf.repository.UserRepository;

/**
 * 自定义Realm
 * @author Administrator
 *
 */
public class MyRealm extends AuthorizingRealm{

	@Resource
	private UserRepository userRepository;
	
	@Resource
	private RoleRepository roleRepository;
	
	@Resource
	private MenuRepository menuRepository;
	
	/**
	 * 授权
	 */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String userName=(String) SecurityUtils.getSubject().getPrincipal();
//		Object primaryPrincipal = principals.getPrimaryPrincipal();
		User user=userRepository.findByUserName(userName);//弊端 每次get到的只是username 还要查一下user
		SimpleAuthorizationInfo info=new SimpleAuthorizationInfo();
		List<Role> roleList=roleRepository.findByUserId(user.getId());//三表查roles
		Set<String> roles=new HashSet<String>();
		for(Role role:roleList){
			roles.add(role.getName());
			List<Menu> menuList=menuRepository.findByRoleId(role.getId());//
			for(Menu menu:menuList){//因为role和permission是多对多所以要2for
				info.addStringPermission(menu.getName());				
			}
		}
		info.setRoles(roles);
		return info;
	}

	/**
	 * 身份权限认证
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		String userName=(String) token.getPrincipal(); //获取name
//		Object credentials = token.getCredentials();
		User user=userRepository.findByUserName(userName);//单纯从t_usr查user对象
		if(user!=null){//如果有这个用户    取出name和password 让shiro判断
			AuthenticationInfo authcInfo=new SimpleAuthenticationInfo(user.getUserName(),user.getPassword(),"xxx");
			return authcInfo;
		}else{
			return null;			
		}
	}

}
