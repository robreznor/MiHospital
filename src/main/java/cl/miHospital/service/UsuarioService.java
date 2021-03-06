package cl.miHospital.service;

import static org.junit.Assert.assertEquals;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import cl.miHospital.model.Institucion;
import cl.miHospital.model.Usuario;
import cl.miHospital.object.JwtUserDto;
import cl.miHospital.object.UserLoginResponse;
import cl.miHospital.object.UsuarioResponse;
import cl.miHospital.util.HibernateUtility;
import cl.miHospital.util.JwtUtil;
import cl.miHospital.util.Utils;

public class UsuarioService {
	
	public static Usuario getUsuario(String rut){
		SessionFactory sessionFactory = HibernateUtility.getSessionFactory();
        Session session = sessionFactory.openSession();
        Query q = session.createQuery("from Usuario u where u.rut='" + rut + "'");
        Usuario usuario =  (Usuario) q.uniqueResult();	 
        return usuario;
	}
	
	public static Set<UsuarioResponse> getUsuarios(){
		SessionFactory sessionFactory = HibernateUtility.getSessionFactory();
        Session session = sessionFactory.openSession();
        Query q = session.createQuery("select u.nombres, u.apellido, u.rut, u.correo, u.telefono, u.institucion from Usuario u where u.es_some='1'");
        List<Object> usuarios =   q.list();
        Set<UsuarioResponse> usuariosResponse = new HashSet<UsuarioResponse>();;
        for(int i =0; i< usuarios.size(); i++){
        	UsuarioResponse usuario = new UsuarioResponse();
        	usuario.setNombres((String)((Object[])usuarios.get(i))[0]);
        	usuario.setApellido((String)((Object[])usuarios.get(i))[1]);
        	usuario.setRut((String)((Object[])usuarios.get(i))[2]);
        	usuario.setCorreo((String)((Object[])usuarios.get(i))[3]);
        	usuario.setTelefono((String)((Object[])usuarios.get(i))[4]);
        	usuario.setInstitucion((Institucion)((Object[])usuarios.get(i))[5]);
        	usuariosResponse.add(usuario);
        }

        return usuariosResponse;
	}

	
	public static boolean insertUsuario(HttpServletRequest request, ObjectNode message){
		String rut = rutFormat(request.getParameter("rut"));
		if(getUsuario(rut)!=null){
			message.put("message", "El RUT ingresado ya existe!");
			return false;
		}
		Usuario usuario = fillUsuario(request);
		try{
			SessionFactory sessionFactory = HibernateUtility.getSessionFactory();
			Session session = sessionFactory.openSession(); 
			Transaction tx1 = session.beginTransaction();
			session.save(usuario);
	        tx1.commit();
	        session.close();
	        return true;
	        
		}catch(Exception e){
			e.printStackTrace();
			message.put("message", "Hubo un problema en el servidor");
			return false;
		}
        
	}
	
	public static boolean updateUsuario(HttpServletRequest request, String rut, ObjectNode message){
		String newRut = rutFormat(request.getParameter("rut"));	
		if(!newRut.equals(rut)){
			if(getUsuario(newRut)!=null){
				message.put("message", "El RUT ingresado ya existe!");
				return false;
			}
		}
		Usuario usuario = getUsuario(rut);
		setUsuario(request, usuario);
		try{
			SessionFactory sessionFactory = HibernateUtility.getSessionFactory();
			Session session = sessionFactory.openSession(); 
			Transaction tx1 = session.beginTransaction();
			session.update(usuario);
	        tx1.commit();
	        session.close();
	        return true;
	        
		}catch(Exception e){
			e.printStackTrace();
			message.put("message", "Hubo un problema en el servidor");
			return false;
		}
	}
	
	public static void setUsuario(HttpServletRequest request, Usuario usuario){
		String nombres =Utils.getStringUTF(request.getParameter("nombres"));
		String apellido = Utils.getStringUTF(request.getParameter("apellido"));
		String correo = request.getParameter("correo");
		String id_institucion = request.getParameter("id_institucion");
		String telefono = request.getParameter("telefono");
		String rut = rutFormat(request.getParameter("rut"));
		Institucion institucion = InstitucionService.getInstitucion(id_institucion);
		usuario.setNombres(nombres);
		usuario.setApellido(apellido);
		usuario.setCorreo(correo);
		usuario.setInstitucion(institucion);
		usuario.setTelefono(telefono);
		usuario.setRut(rut);
	}
	
	public static Usuario fillUsuario(HttpServletRequest request){
		
		String nombres = Utils.getStringUTF(request.getParameter("nombres"));
		String apellido = Utils.getStringUTF(request.getParameter("apellido"));
		String correo = request.getParameter("correo");
		String contrasena = DigestUtils.md5Hex(Utils.getStringUTF(request.getParameter("contrasena")));
		String creado_por = request.getParameter("creado_por");
		String id_institucion = request.getParameter("id_institucion");
		String telefono = request.getParameter("telefono");
		String rut = rutFormat(request.getParameter("rut"));
		Institucion institucion = InstitucionService.getInstitucion(id_institucion);
		Usuario usuario = new Usuario();
		usuario.setNombres(nombres);
		usuario.setApellido(apellido);
		usuario.setContrasena(contrasena);
		usuario.setCorreo(correo);
		usuario.setCreado_por(creado_por);
		usuario.setEs_some(1);
		usuario.setEsPaciente(0);
		usuario.setInstitucion(institucion);
		usuario.setPrimer_ingreso(0);
		usuario.setTelefono(telefono);
		usuario.setRut(rut);
		
		return usuario;
	}
	
	public static boolean deleteUsuario(String rut){
		try{
			SessionFactory sessionFactory = HibernateUtility.getSessionFactory();
	        Session session = sessionFactory.openSession();
	        Transaction tx1 = session.beginTransaction();
	        Usuario usuario = getUsuario(rut);
			session.delete(usuario);
	        tx1.commit();
	        return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}		
	}
	
	public static Usuario correctPassword(String rut, String password, ObjectNode message){
		rut =rutFormat(rut);
		try{
			Usuario user = getUsuario(rut);
			if(user==null){
				message.put("message", "La combinación usuario y contraseña no es correcta");
			}
			else if(DigestUtils.md5Hex(password).equals(user.getContrasena())){
				return user;
			}else{
				message.put("message", "La combinación usuario y contraseña no es correcta");
			}
		}catch(Exception e){
			e.printStackTrace();
			message.put("message", "El servidor no está disponible");
		}
		return null;
	}
	
	private static String rutFormat(String rut){
		String rutAux = rut.replace(".", "");
		if(rut.split("-").length<2){
			rutAux= rut.substring(0, rut.length()-1) + "-" +rut.substring(rut.length()-1);	
		}
		return rutAux;
	}
	
	public static UserLoginResponse fillUserResponse (Usuario user){
		JwtUtil jwtutil = new JwtUtil();
		JwtUserDto userDto = new JwtUserDto();
		Long id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
		userDto.setId(id);
		userDto.setRole("some");
		userDto.setUsername(user.getNombres());
		String token = jwtutil.generateToken(userDto);
		UserLoginResponse userRes = new UserLoginResponse();
		userRes.setUsername(user.getNombres());
		userRes.setRut(user.getRut());
		userRes.setToken(token);
		userRes.setRole("some");
		
		return userRes;
		
	}
	
	public static boolean SetPassword(HttpServletRequest request, ObjectNode message, String rut){
//		String oldPassword = Utils.getStringUTF(request.getParameter("old_password"));
		String newPassword = Utils.getStringUTF(request.getParameter("new_password"));
		Usuario usuario = getUsuario(rut);
		if(usuario!=null){
			usuario.setContrasena(DigestUtils.md5Hex(newPassword));
			try{
				SessionFactory sessionFactory = HibernateUtility.getSessionFactory();
				Session session = sessionFactory.openSession(); 
				Transaction tx1 = session.beginTransaction();
				session.update(usuario);
		        tx1.commit();
		        session.close();
		        return true;
			}catch(Exception e){
				e.printStackTrace();
				message.put("message", "Hubo un problema en el servidor");
				return false;
			}	
		}else{
			message.put("message", "El rut ingresado no existe en los registros");
			return false;
		}
		
	}
	
	private static boolean passwordMatch(String password, Usuario usuario){
		if(usuario!=null){
			return (DigestUtils.md5Hex(password).equals(usuario.getContrasena()));
		}
		return false;
	}
	
	@Test
	public void getUser(){
		//assertEquals(null, getUsuario("asdas"));
		//assertEquals(true, rutFormat("16.332.2331").equals("16332233-1"));
	}

}
