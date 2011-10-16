package gambioc

import au.com.bytecode.opencsv.CSVReader
import com.lucastex.grails.fileuploader.UFile

class ConvertController {

    def index = {
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

        def reader = new CSVReader(file.newReader ('ISO-8859'), ';' as char)

        def downloadDir = new File("/tmp/images_${new Date().time}")
        if (!downloadDir.exists() && !downloadDir.mkdir())
            throw new FileNotFoundException(downloadDir, "path does not exists and cannot be created, check permissions")

        def ofile
        (ofile = new File("/tmp/${file.name}")).withWriter { out ->
	        reader.readAll().eachWithIndex { line, idx ->
	            if(idx){
                    out.println convertLine(line, f, downloadDir)
	            }
	        }
		}

        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "attachment;filename=/tmp/${file.name}")
        response.outputStream << ofile.newInputStream()
    }


    private String convertLine (String[] resource, double factor, File downloadPath) {
        def rtn = ['']

        rtn << resource[1]
        rtn << resource[9]
        rtn << ''
        rtn << ''
        rtn << ''
        rtn << ''
        rtn << ''
        rtn << ''
        rtn << resource[0]
        rtn << ((resource[10] =~ /(?)erotik/)? 0 : 1)
        rtn << (((resource[4]-'EUR').trim() as double) * factor)
        rtn << 1
        rtn << (((resource[9] as int) > 0)? 1 : 0)
        rtn << resource[8]
        rtn << ((resource[7] ==~ /9+/)? null : resource[7])
        rtn << ''
        rtn << ''
        rtn << downloadImage(resource[6].toURL(), downloadPath)
        rtn << resource[2]
        rtn << resource[11]
        rtn << ''
        rtn << resource[2]
        rtn << resource[11]
        rtn << ''
        rtn << resource[5]

        def categories = []
        categories += (resource[10].split ('/') ?: [])
        def s = categories.size()
        if (s < 7) {
            s.upto(7) {
                categories << ''
            }
        }

        categories.each {
            rtn << it
        }

        rtn << ''
        rtn << 1
        rtn << ''

        rtn.join ('|')
    }

    // downloads file behind url, returns simple file name
    // below /tmp/images_$TIMESTAMP
    private String downloadImage(URL url, File path) {
        log.debug "going to download $url"

        def t = new Date().time
        try {
	        def ext = url.file.substring(url.file.lastIndexOf('/')).replaceAll (/^[^\.]*/, '')
	        def o = new BufferedOutputStream(new FileOutputStream(new File(path, "$t$ext")))
	        o << url.openStream ()
	        o.close ()
        } catch (IOException _e) {
            log.warn _e.message
            return ''
        }

        log.debug "downloaded $url to $t"
        "$t"
    }
}
