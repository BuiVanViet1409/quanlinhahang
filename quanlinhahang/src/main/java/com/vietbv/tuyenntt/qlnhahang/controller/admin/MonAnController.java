package com.vietbv.tuyenntt.qlnhahang.controller.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.vietbv.tuyenntt.qlnhahang.domain.MonAn;
import com.vietbv.tuyenntt.qlnhahang.domain.NhomMonAn;
import com.vietbv.tuyenntt.qlnhahang.model.MonAnDto;
import com.vietbv.tuyenntt.qlnhahang.model.NhomMonAnDto;
import com.vietbv.tuyenntt.qlnhahang.service.MonAnService;
import com.vietbv.tuyenntt.qlnhahang.service.NhomMonAnService;
import com.vietbv.tuyenntt.qlnhahang.service.StorageService;

@Controller
@RequestMapping("admin/monan")
public class MonAnController {

	@Autowired
	NhomMonAnService _nhomMonAnService;

	@Autowired
	MonAnService _monAnService;

	@Autowired
	StorageService _storageService;

	@ModelAttribute("nhomMonAns")
	public List<NhomMonAnDto> getNhomMonAns() {
		return _nhomMonAnService.findAll().stream().map(item -> {
			NhomMonAnDto dto = new NhomMonAnDto();
			BeanUtils.copyProperties(item, dto);// copy nh??m m??n t??? item sang dto
			return dto;
		}).collect(Collectors.toList()); // chuy???n to??n b??? danh s??ch c???a dto sang list
	}

	@GetMapping("add")
	public String add(Model model) {
		MonAnDto dto = new MonAnDto();
		dto.setIsEdit(false);
		model.addAttribute("monAn", dto);
		return "admin/monans/addOrEdit";
	}
	
	@RequestMapping("listall")
	public String list(ModelMap model) {
		// tr??? v??? danh s??ch c??c category
		List<NhomMonAn> list = _nhomMonAnService.findAll();
		
		model.addAttribute("nhomMons", list);
		return "admin/monans/list";
	}
	
	@GetMapping("list")
	public String menu(ModelMap model) {
		// tr??? v??? danh s??ch c??c category
		List<NhomMonAn> list = _nhomMonAnService.findAll();
		
		model.addAttribute("nhomMonAns", list);
		return "admin/monans/listByCategory";
	}
	
	@GetMapping("/{maNhomMon}")
	public ModelAndView monAn(ModelMap model, @PathVariable("maNhomMon") Long maNhomMon) {
		List<MonAn> list = _monAnService.findByMaNhomMonAn(maNhomMon);
		model.addAttribute("monAnByCategory", list);
		return new ModelAndView("forward:/admin/monan/list",model);
	}

	// th??m m???i v?? ch???nh s???a th??ng tin
	@PostMapping("saveOrUpdate")
	public ModelAndView saveOrUpdate(ModelMap model, @Valid @ModelAttribute("product") MonAnDto dto,
			BindingResult result) {

		if (result.hasErrors()) {
			return new ModelAndView("admin/monans/addOrEdit");
		}

		MonAn entity = new MonAn();
		BeanUtils.copyProperties(dto, entity); // copy n???i dung t??? dto sang entity(h???n ch??? vi???c get set nhi???u l???n)

		// th??m th??ng tin category v??o productCategory
		NhomMonAn category = new NhomMonAn();
		category.setMaNhomMon(dto.getMaNhomMon());
		entity.setNhomMonAn(category);

		// l??u n???i dung c???a img ??c upload t???i server
		if (!dto.getImageFile().isEmpty()) {
			// cho ph??p sinh ra 1 d??y k?? t??? d??ng ????? nh???n d???ng
			UUID uuid = UUID.randomUUID();
			String uuString = uuid.toString();

			entity.setImage(_storageService.getStoredFileName(dto.getImageFile(), uuString));
			// x??c ?????nh t??n file c???n l??u tr???
			_storageService.store(dto.getImageFile(), entity.getImage());
		}

		_monAnService.save(entity);// l??u th??ng tin c???a entity v??o DB

		model.addAttribute("message", "M??n ??n ???? l??u!");// hi???n th??? th??ng b??o
		return new ModelAndView("forward:/admin/monan/searchpaginated", model);
	}

	@RequestMapping("searchpaginated")
	public ModelAndView search(ModelMap model, @RequestParam(name = "tenMonAn", required = false) String tenMonAn,
			@RequestParam("page") Optional<Integer> page, @RequestParam("size") Optional<Integer> size) {

		/*
		 * currentPage: trang hi???n t???i pageSize: k??ch th?????c c???a trang
		 */
		int currentPage = page.orElse(1);// ng?????i d??ng ko nh???p gi?? tr???, gi?? tr??? ng???m ?????nh l?? 1
		int pageSize = size.orElse(5);// gi?? tr??? ng???m ?????nh 5 ph???n t??? tr??n 1 trang

		Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by("tenMonAn"));// s???p x???p theo field name
		Page<MonAn> resultPage = null;

		if (StringUtils.hasText(tenMonAn)) { // n???u name c?? gi?? tr???
			resultPage = _monAnService.findByTenMonAnContaining(tenMonAn, pageable); // name: t??n c???n t??m ki???m.
																						// pageable: c??c th??ng s??? ph??n
																						// trang v?? s???p x???p
			model.addAttribute("tenMonAn", tenMonAn);
		} else {
			resultPage = _monAnService.findAll(pageable);
		}

		int totalPage = resultPage.getTotalPages();// s??? trang ???????c hi???n th??? tr??n view(bn trang)
		if (totalPage > 0) {
			int start = Math.max(1, currentPage - 2);
			int end = Math.min(currentPage + 2, totalPage);

			if (totalPage > 5) {
				if (end == totalPage)
					start = end - 5;
				else if (start == 1)
					end = start + 5;
			}
			/*
			 * .collector(Collectors.toList()): chuy???n c??c gi?? tr??? sinh ra (start, end)
			 * th??nh danh s??ch
			 * 
			 */
			List<Integer> pageNumbers = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());

			// danh s??ch c??c gi?? tr??? Integer ????? t??nh ra s??? trang c???n hi???n th??? tr??n view
			model.addAttribute("pageNumbers", pageNumbers);
		}

		// tr??? v??? k???t qu??? ph????ng th???c t??m ki???m
		model.addAttribute("productPage", resultPage);

		return new ModelAndView("forward:/admin/monan/listall", model); 
	}

	// ch???nh s???a form addOrEdit
	@GetMapping("edit/{maMonAn}")
	public ModelAndView edit(ModelMap model, @PathVariable("maMonAn") Long maMonAn) {

		Optional<MonAn> opt = _monAnService.findById(maMonAn);// t??m th??? c?? id ???? ko
		MonAnDto dto = new MonAnDto();

		if (opt.isPresent()) {// c?? d??? li???u tr??? v???
			MonAn entity = opt.get();// l???y d??? li???u ???? trong DB
			BeanUtils.copyProperties(entity, dto);// copy t??? entity sang dto
			// l???y categoryId ????? thi???t l???p cho tr?????ng categoryProductDto
			dto.setMaNhomMon(entity.getNhomMonAn().getMaNhomMon());
			dto.setIsEdit(true);// b??n edit th?? true

			model.addAttribute("monAn", dto);

			return new ModelAndView("admin/monans/addOrEdit", model);
		}

		model.addAttribute("message", "M??n ??n kh??ng t???n t???i");

		return new ModelAndView("forward:/admin/monan/searchpaginated", model); // n???u ko c?? th?? chuy???n t???i list
	}
	
	@GetMapping("/images/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename){ //?????c n???i dung filename ??c g???i t???i
		Resource file = _storageService.loadAsResource(filename);
		
		//tr??? v??? n???i dung c???a filename ????
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}
	
	@GetMapping("delete/{maMonAn}")
	public ModelAndView delete(ModelMap model, @PathVariable("maMonAn") Long maMonAn) {
		
		_monAnService.deleteById(maMonAn);
		
		model.addAttribute("message", "M??n ??n ???? b??? x??a!");
		
		return new ModelAndView("forward:/admin/monan/searchpaginated",model);
	}
}
