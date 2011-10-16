package gambioc

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

        log.debug "factor will be ${params.factor}"


        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "attachment;filename=${file.getName()}")
        response.outputStream << file.newInputStream()
    }
}
