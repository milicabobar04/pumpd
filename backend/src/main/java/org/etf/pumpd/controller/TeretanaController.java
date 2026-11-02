package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.service.*;

@RestController
@RequestMapping("/api/gym")
public class TeretanaController {

    private final TeretanaService teretanaService;

    public TeretanaController(TeretanaService teretanaService) {
        this.teretanaService = teretanaService;
    }

    @GetMapping("/info")
    public InfoTeretanaDto informacije() {
        return teretanaService.informacije();
    }
}