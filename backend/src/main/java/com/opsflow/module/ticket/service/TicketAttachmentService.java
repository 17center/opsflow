package com.opsflow.module.ticket.service;

import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.ticket.mapper.TicketAttachmentMapper;
import com.opsflow.module.ticket.mapper.TicketMapper;
import com.opsflow.module.ticket.model.entity.Ticket;
import com.opsflow.module.ticket.model.entity.TicketAttachment;
import com.opsflow.module.ticket.model.vo.AttachmentVO;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 工单附件服务：上传至 MinIO、下载、记录附件元数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAttachmentService {

    /** 单文件大小上限 20MB */
    private static final long MAX_SIZE = 20 * 1024 * 1024L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final TicketMapper ticketMapper;
    private final TicketAttachmentMapper attachmentMapper;
    private final MinioClient minioClient;

    @Value("${opsflow.minio.bucket:opsflow}")
    private String bucket;

    /**
     * 上传附件到 MinIO 并记录元数据
     */
    @Transactional
    public AttachmentVO upload(Long ticketId, MultipartFile file, Long operatorId, String operatorName) {
        getTicket(ticketId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.ATTACHMENT_TOO_LARGE);
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String objectKey = "tickets/" + LocalDate.now().format(DATE_FMT) + "/" + UUID.randomUUID() + ext;
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("上传附件到 MinIO 失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "附件上传失败");
        }

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(ticketId);
        attachment.setFileName(originalName);
        attachment.setFilePath(objectKey);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setUploaderId(operatorId);
        attachment.setCreateBy(operatorName);
        attachmentMapper.insert(attachment);

        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setFileName(attachment.getFileName());
        vo.setFilePath(attachment.getFilePath());
        vo.setFileSize(attachment.getFileSize());
        vo.setUploadTime(attachment.getCreateTime());
        return vo;
    }

    /**
     * 下载附件（返回文件名与文件字节）
     */
    public DownloadResult download(Long ticketId, Long attachmentId) {
        getTicket(ticketId);
        TicketAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "附件不存在");
        }
        try {
            try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(attachment.getFilePath())
                    .build())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                return new DownloadResult(attachment.getFileName(), out.toByteArray());
            }
        } catch (Exception e) {
            log.error("下载附件失败: id={}", attachmentId, e);
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "附件下载失败");
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private Ticket getTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "工单不存在");
        }
        return ticket;
    }

    /**
     * 下载结果：文件名 + 文件字节
     */
    public record DownloadResult(String fileName, byte[] content) {
    }
}