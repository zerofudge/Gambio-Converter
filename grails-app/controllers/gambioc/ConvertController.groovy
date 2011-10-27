package gambioc

import au.com.bytecode.opencsv.CSVReader
import com.lucastex.grails.fileuploader.UFile

class ConvertController {
	static tmp = System.properties['java.io.tmpdir']
    static long cnt = 0

    def index = {
        if (params.ufileId)
	        log.debug "Uploaded file with id=${params.ufileId}"
        [files: UFile.list(), params:params]
    }

    def delete = {
        def ufile = UFile.get(params.id)
        ufile.delete()
        redirect action:index
    }

    def convert = {
        def ufile = UFile.get(params.id)
        def file = new File(ufile.path)

        def f = params.factor as double
        log.debug "factor will be $f"

        def reader = new CSVReader(file.newReader ('ISO-8859-1'), ';' as char)

        def downloadDir = new File("/$tmp/images_${new Date().time}")
        if (!downloadDir.exists() && !downloadDir.mkdir())
            throw new FileNotFoundException(downloadDir, "path does not exists and cannot be created, check permissions")

        def ofile
        (ofile = new File("/$tmp/${file.name}")).withWriter('ISO-8859-1') { out ->
	        reader.readAll().eachWithIndex { line, idx ->
	            if(idx){
                    out.println convertLine(line, f, downloadDir)
	            }
	        }
		}

        response.setContentType('application/octet-stream;charset=ISO-8859-1')
        response.setHeader ('Encoding', 'ISO-8829-1')
        response.setHeader("Content-disposition", "attachment;filename=${ofile.name}")
        response.outputStream << ofile.newInputStream()
    }


    private String convertLine (String[] resource, double factor, File downloadPath) {
        // 0 fix
        def rtn = ['XTSOL']

        // 1 p_id
        rtn << ++cnt
        // 2 p_model
        rtn << resource[1]
        // 3 p_stock
        rtn << String.format ('%.4f', resource[9] as double)
        // 4 p_sorting
        rtn << 0
        // 5 p_startpage
        rtn << 0
        // 6 p_startpage_sort
        rtn << 0
        // 7 p_shipping
        rtn << 1
        // 8 p_tpl
        rtn << 'default'
        // 9 p_opttpl
        rtn << 'default'
        // 10 p_manufacturer
        rtn << resource[0]
        // 11 p_fsk18
        rtn << ((resource[10] =~ /(?)erotik/)? 1 : 0)
        // 12 p_priceNoTax
        rtn << String.format ('%.4f', ((resource[4]-'EUR').trim() as double) * factor)
        // 13 p_priceNoTax.1
        rtn << ''
        // 14 p_priceNoTax.2
        rtn << ''
        // 15 p_priceNoTax.3
        rtn << ''
        // 16 p_tax
        rtn << 1
        // 17 p_status
        rtn << (((resource[9] as int) > 0)? 1 : 0)
        // 18 p_weight
        rtn << String.format ('%.2f', (resource[8]-'kg').trim() as double)
        // 19 p_ean
        rtn << ((resource[7] ==~ /9+/)? '' : "${resource[7]}" )
        // 20 p_disc
        rtn << String.format ('%.2f', 0 as double)
        // 21 p_date_added
        def d = new Date()
        rtn << d.format('yyyy-MM-dd hh:mm:ss')
        // 22 p_last_modified
        rtn << d.format('yyyy-MM-dd hh:mm:ss')
        // 23 p_date_available
        rtn << ''
        // 24 p_ordered
        rtn << String.format ('%.4f', 0 as double)
        // 25 nc_ultra_shipping_costs
        rtn << ''
        // 26 gm_show_date_added
        rtn << 0
        // 27 gm_show_price_offer
        rtn << 0
        // 28 gm_show_qty_info
        rtn << 0
        // 29 gm_price_status
        rtn << 0
        // 30 gm_min_order
        rtn << String.format ('%.4f', 1 as double)
        // 31 gm_graduated_qty
        rtn << String.format ('%.4f', 1 as double)
        // 32 gm_options_template
        rtn << 'default'
        // 33 p_vpe
        rtn << 0
        // 34 p_vpe_status
        rtn << 0
        //35 p_vpe_value
        rtn << String.format ('%.4f', 0 as double)
        // 36 p_image.1
        rtn << ''
        // 37 p_image.2
        rtn << ''
        // 38 p_image.3
        rtn << ''
        // 39 p_image
        rtn << downloadImage(resource[6].toURL(), downloadPath)
        // 40 p_name.en
        rtn << "${resource[2].replace('|', ',')}"
        // 41 p_desc.en
        rtn << "<p>${resource[11].replace('|', ',')}</p>"
        // 42 p_shortdesc.en
        rtn << "<p>${resource[2].replace('|', ',')}</p>"
        // 43 p_meta_title.en
        rtn << "${resource[2].replace('|', ',')}"
        // 44 p_meta_desc.en
        rtn << "${resource[11].replace('|', ',')}"
        // 45 p_meta_key.en
        rtn << "${resource[2].replace('|', ',')}".split (' ').join(',')
        // 46 p_keywords.en
        rtn << ''
        // 47 p_url.en
        rtn << "\"${resource[5]}\""
        // 48 p_name.de
        rtn << "${resource[2].replace('|', ',')}"
        // 49 p_desc.de
        rtn << "<p>${resource[11].replace('|', ',')}</p>"
        // 50 p_shortdesc.de
        rtn << "<p>${resource[2].replace('|', ',')}</p>"
        // 51 p_meta_title.de
        rtn << "${resource[2].replace('|', ',')}"
        // 52 p_meta_desc.de
        rtn << "${resource[11].replace('|', ',')}"
        // 53 p_meta_key.de
        rtn << "${resource[2].replace('|', ',')}".split (' ').join(',')
        // 54 p_keywords.de
        rtn << ''
        // 55 p_url.de
        rtn << "\"${resource[5]}\""

        // 56..61 p_cat0..5
        def categories = (resource[10].split ('/').flatten() ?: [])
        def s = categories.size()
        if (s < 5) {
            (s+1).upto(5) {
                categories << ''
            }
        }
        categories.each { rtn << it }

        rtn.join ('|')
    }

    // downloads file behind url, returns simple file name
    // below /tmp/images_$TIMESTAMP
    private String downloadImage(URL url, File path) {
        log.debug "going to download $url"

        def t = new Date().time
        def ext = url.file.substring(url.file.lastIndexOf('/')).replaceAll (/^[^\.]*/, '') ?: '.jpg'

        try {
	        def o = new BufferedOutputStream(new FileOutputStream(new File(path, "$t$ext")))
	        o << url.openStream ()
	        o.close ()
        } catch (IOException _e) {
            log.warn "${_e.class.simpleName}: _e.message"
            return ''
        }

        log.debug "downloaded $url to $t$ext"
        "$t$ext"
    }
}
